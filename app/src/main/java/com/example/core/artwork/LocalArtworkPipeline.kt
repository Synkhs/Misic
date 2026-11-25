package com.example.core.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.Immutable
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.imageLoader
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.Options
import coil.decode.ImageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.Path.Companion.toOkioPath
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@Immutable
data class LocalArtworkRequest(
    val trackId: Long,
    val albumId: Long,
    val contentUri: String,
    val albumArtUri: String,
    val filePath: String = "",
    val dateModifiedSec: Long = 0L,
    val targetSizePx: Int = 128,
)

enum class LocalArtworkSourceKind {
    DISK_CACHE,
    LOAD_THUMBNAIL,
    EMBEDDED_METADATA,
    FOLDER_ART,
    LEGACY_ALBUM_ART_URI,
    NONE,
}

@Immutable
data class LocalArtworkDiagnostics(
    val cacheHits: Int = 0,
    val decodedCount: Int = 0,
    val lastSource: LocalArtworkSourceKind = LocalArtworkSourceKind.NONE,
    val lastDecodeMs: Long = 0L,
    val lastTrackId: Long = -1L,
)

object LocalArtworkPipeline {
    private val _diagnostics = MutableStateFlow(LocalArtworkDiagnostics())
    val diagnostics: StateFlow<LocalArtworkDiagnostics> = _diagnostics.asStateFlow()

    private val failureNegativeCache = LruCache<String, Long>(256)

    @Volatile
    private var sharedImageLoader: ImageLoader? = null

    fun cacheKeyFor(request: LocalArtworkRequest): String =
        "${request.trackId}_${request.dateModifiedSec}_${request.targetSizePx}"

    fun getOrCreateImageLoader(context: Context): ImageLoader {
        sharedImageLoader?.let { return it }
        synchronized(this) {
            sharedImageLoader?.let { return it }
            val appCtx = context.applicationContext
            val diskDir = File(appCtx.cacheDir, "magic_coil_cache")
            val loader = ImageLoader.Builder(appCtx)
                .components {
                    add(LocalArtworkKeyer())
                    add(LocalArtworkFetcher.Factory(appCtx))
                }
                .memoryCache {
                    MemoryCache.Builder(appCtx)
                        .maxSizePercent(0.20)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(diskDir.toOkioPath())
                        .maxSizeBytes(48L * 1024L * 1024L)
                        .build()
                }
                .crossfade(false)
                .build()
            sharedImageLoader = loader
            return loader
        }
    }

    suspend fun resolveAndCacheBytes(
        context: Context,
        request: LocalArtworkRequest,
    ): Pair<ByteArray?, LocalArtworkSourceKind> = withContext(Dispatchers.IO) {
        val startNs = System.nanoTime()
        val key = cacheKeyFor(request)
        val diskFile = cachedThumbFile(context, key)

        if (diskFile.exists() && diskFile.length() > 64L) {
            val bytes = runCatching { diskFile.readBytes() }.getOrNull()
            if (bytes != null && bytes.isNotEmpty()) {
                recordStats(LocalArtworkSourceKind.DISK_CACHE, startNs, request.trackId, isHit = true)
                return@withContext bytes to LocalArtworkSourceKind.DISK_CACHE
            }
        }

        // Skip re-probing tracks that recently failed within 10 minutes
        val prevFail = failureNegativeCache.get(key)
        if (prevFail != null && System.currentTimeMillis() - prevFail < 600_000L) {
            return@withContext null to LocalArtworkSourceKind.NONE
        }

        var resultBytes: ByteArray? = null
        var sourceKind = LocalArtworkSourceKind.NONE

        // 1. Android 10+ ContentResolver.loadThumbnail (fast OS binder thumbnail)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val size = Size(request.targetSizePx.coerceIn(96, 512), request.targetSizePx.coerceIn(96, 512))
            val contentThumb = runCatching {
                if (request.contentUri.isNotBlank()) {
                    context.contentResolver.loadThumbnail(Uri.parse(request.contentUri), size, null)
                } else {
                    null
                }
            }.getOrNull() ?: runCatching {
                if (request.albumArtUri.isNotBlank()) {
                    context.contentResolver.loadThumbnail(Uri.parse(request.albumArtUri), size, null)
                } else {
                    null
                }
            }.getOrNull()
            if (contentThumb != null && !contentThumb.isRecycled) {
                resultBytes = compressBitmap(contentThumb, request.targetSizePx)
                sourceKind = LocalArtworkSourceKind.LOAD_THUMBNAIL
            }
        }

        // 2. Embedded metadata via MediaMetadataRetriever
        if (resultBytes == null && request.contentUri.isNotBlank()) {
            val embedded = extractEmbeddedBytes(context, request.contentUri)
            if (embedded != null && embedded.isNotEmpty()) {
                resultBytes = decodeAndScaleToJpeg(embedded, request.targetSizePx) ?: embedded
                sourceKind = LocalArtworkSourceKind.EMBEDDED_METADATA
            }
        }

        // 3. Folder cover art fallback (cover.jpg, folder.jpg, front.jpg)
        if (resultBytes == null && request.filePath.isNotBlank()) {
            val folderBytes = extractFolderArtBytes(request.filePath, request.targetSizePx)
            if (folderBytes != null) {
                resultBytes = folderBytes
                sourceKind = LocalArtworkSourceKind.FOLDER_ART
            }
        }

        // 4. Legacy MediaStore album art URI fallback
        if (resultBytes == null && request.albumArtUri.isNotBlank()) {
            val streamBytes = runCatching {
                context.contentResolver.openInputStream(Uri.parse(request.albumArtUri))?.use { it.readBytes() }
            }.getOrNull()
            if (streamBytes != null && streamBytes.isNotEmpty()) {
                resultBytes = decodeAndScaleToJpeg(streamBytes, request.targetSizePx) ?: streamBytes
                sourceKind = LocalArtworkSourceKind.LEGACY_ALBUM_ART_URI
            }
        }

        if (resultBytes != null && resultBytes.isNotEmpty()) {
            writeCacheAtomically(diskFile, resultBytes)
            failureNegativeCache.remove(key)
            recordStats(sourceKind, startNs, request.trackId, isHit = false)
            resultBytes to sourceKind
        } else {
            failureNegativeCache.put(key, System.currentTimeMillis())
            recordStats(LocalArtworkSourceKind.NONE, startNs, request.trackId, isHit = false)
            null to LocalArtworkSourceKind.NONE
        }
    }

    fun clearCache(context: Context) {
        failureNegativeCache.evictAll()
        val dir = File(context.cacheDir, "magic_artwork_thumbs")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    private fun extractEmbeddedBytes(context: Context, contentUri: String): ByteArray? {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, Uri.parse(contentUri))
            mmr.embeddedPicture
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { mmr.release() }
        }
    }

    private fun extractFolderArtBytes(filePath: String, targetSizePx: Int): ByteArray? {
        val parent = runCatching { File(filePath).parentFile }.getOrNull() ?: return null
        if (!parent.exists() || !parent.isDirectory) return null
        val candidates = listOf("cover.jpg", "cover.png", "folder.jpg", "folder.png", "front.jpg", "album.jpg")
        val artFile = candidates.asSequence()
            .map { File(parent, it) }
            .firstOrNull { it.exists() && it.canRead() && it.length() in 128L..(12L * 1024L * 1024L) }
            ?: return null
        val raw = runCatching { artFile.readBytes() }.getOrNull() ?: return null
        return decodeAndScaleToJpeg(raw, targetSizePx)
    }

    private fun decodeAndScaleToJpeg(raw: ByteArray, targetSizePx: Int): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        if (maxDim <= 0) return null

        var sample = 1
        while (maxDim / sample > targetSizePx * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bmp = BitmapFactory.decodeByteArray(raw, 0, raw.size, opts) ?: return null
        return try {
            compressBitmap(bmp, targetSizePx)
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
        }
    }

    private fun compressBitmap(source: Bitmap, targetSizePx: Int): ByteArray? {
        val scaled = if (maxOf(source.width, source.height) > targetSizePx) {
            val scale = targetSizePx.toFloat() / maxOf(source.width, source.height).toFloat()
            val w = (source.width * scale).toInt().coerceAtLeast(32)
            val h = (source.height * scale).toInt().coerceAtLeast(32)
            Bitmap.createScaledBitmap(source, w, h, true)
        } else {
            source
        }
        return try {
            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 86, out)
                out.toByteArray()
            }
        } finally {
            if (scaled !== source && !scaled.isRecycled) {
                scaled.recycle()
            }
        }
    }

    private fun cachedThumbFile(context: Context, key: String): File {
        val dir = File(context.cacheDir, "magic_artwork_thumbs")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$key.jpg")
    }

    private fun writeCacheAtomically(target: File, bytes: ByteArray) {
        runCatching {
            val tmp = File(target.parentFile, "${target.name}.tmp")
            FileOutputStream(tmp).use { it.write(bytes) }
            if (!tmp.renameTo(target)) {
                FileOutputStream(target).use { it.write(bytes) }
                tmp.delete()
            }
        }
    }

    private fun recordStats(kind: LocalArtworkSourceKind, startNs: Long, trackId: Long, isHit: Boolean) {
        val ms = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(0L)
        val cur = _diagnostics.value
        _diagnostics.value = cur.copy(
            cacheHits = cur.cacheHits + if (isHit) 1 else 0,
            decodedCount = cur.decodedCount + if (!isHit && kind != LocalArtworkSourceKind.NONE) 1 else 0,
            lastSource = kind,
            lastDecodeMs = ms,
            lastTrackId = trackId,
        )
    }
}

class LocalArtworkKeyer : Keyer<LocalArtworkRequest> {
    override fun key(data: LocalArtworkRequest, options: Options): String =
        LocalArtworkPipeline.cacheKeyFor(data)
}

class LocalArtworkFetcher(
    private val context: Context,
    private val data: LocalArtworkRequest,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val (bytes, sourceKind) = LocalArtworkPipeline.resolveAndCacheBytes(context, data)
        if (bytes == null || bytes.isEmpty()) return null
        val buffer = Buffer().write(bytes)
        val ds = if (sourceKind == LocalArtworkSourceKind.DISK_CACHE) DataSource.DISK else DataSource.DISK
        return SourceResult(
            source = ImageSource(buffer, context),
            mimeType = "image/jpeg",
            dataSource = ds,
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<LocalArtworkRequest> {
        override fun create(
            data: LocalArtworkRequest,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = LocalArtworkFetcher(context.applicationContext, data)
    }
}
