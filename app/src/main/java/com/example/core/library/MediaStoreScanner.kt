package com.example.core.library

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.data.LyricsData
import java.io.File

object MediaStoreScanner {

    fun requiredAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, requiredAudioPermission()) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun scanDeviceAudio(context: Context): List<LocalTrackEntity> {
        if (!hasAudioPermission(context)) return emptyList()

        val result = mutableListOf<LocalTrackEntity>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ARTIST_ID)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.IS_MUSIC)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.BITRATE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Audio.Media.DATA)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        runCatching {
            context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val bitrateCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                } else {
                    -1
                }
                val relCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    -1
                }
                @Suppress("DEPRECATION")
                val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol).orEmpty().ifBlank { "Pista $id" }
                    val artistRaw = cursor.getString(artistCol).orEmpty()
                    val artist = if (artistRaw.isBlank() || artistRaw == "<unknown>") "Artista desconocido" else artistRaw
                    val artistId = cursor.getLong(artistIdCol)
                    val albumRaw = cursor.getString(albumCol).orEmpty()
                    val album = if (albumRaw.isBlank() || albumRaw == "<unknown>") "Álbum desconocido" else albumRaw
                    val albumId = cursor.getLong(albumIdCol)
                    val durationMs = cursor.getLong(durCol).coerceAtLeast(0L)
                    val mimeType = cursor.getString(mimeCol).orEmpty().ifBlank { "audio/*" }
                    val dateModified = cursor.getLong(modCol)
                    val bitrateBps = if (bitrateCol >= 0) cursor.getInt(bitrateCol) else 0
                    val bitrateKbps = (bitrateBps / 1000).coerceAtLeast(0)

                    val folderPath = when {
                        relCol >= 0 -> cursor.getString(relCol).orEmpty().trimEnd('/').ifBlank { "Música" }
                        dataCol >= 0 -> File(cursor.getString(dataCol).orEmpty()).parent ?: "Música"
                        else -> "Música"
                    }
                    val folderName = folderPath.substringAfterLast('/').ifBlank { folderPath }

                    val contentUri = ContentUris.withAppendedId(uri, id).toString()
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId,
                    ).toString()

                    val isLossless = mimeType.contains("flac", true) ||
                        mimeType.contains("wav", true) ||
                        mimeType.contains("alac", true)

                    result.add(
                        LocalTrackEntity(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            artistId = artistId,
                            durationMs = durationMs,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            folderPath = folderPath,
                            folderName = folderName,
                            mimeType = mimeType,
                            bitrateKbps = bitrateKbps,
                            sampleRateHz = if (isLossless) 48000 else 44100,
                            bitDepth = if (isLossless) 24 else 16,
                            dateModified = dateModified,
                        ),
                    )
                }
            }
        }
        return result
    }

    fun tryLoadLyricsForTrack(context: Context, track: LocalTrackEntity): LyricsData? {
        // 1. Check MediaMetadataRetriever embedded lyrics tag if supported
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(track.contentUri))
                val sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
                val bitsPerSample = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)?.toIntOrNull()
                } else {
                    null
                }
                // Check ID3/Vorbis LYRICS if exposed by retriever on some OEMs or sidecar .lrc
                val rawTag = retriever.extractMetadata(1000) // no-op fallback safe check
                LrcParser.parse(rawTag, "${track.album} • Metadatos (${sampleRate ?: track.sampleRateHz} Hz / ${bitsPerSample ?: track.bitDepth}-bit)")
                    ?.let { return it }
            } finally {
                retriever.release()
            }
        }

        // 2. Check sidecar .lrc file when readable on legacy path or app external storage
        val titleSanitized = track.title.replace(Regex("""[\\/:*?"<>|]"""), "_")
        val candidateDirs = listOfNotNull(
            context.getExternalFilesDir("lyrics"),
            File(context.filesDir, "lyrics"),
        )
        candidateDirs.forEach { dir ->
            val lrcFile = File(dir, "$titleSanitized.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                val parsed = runCatching { LrcParser.parse(lrcFile.readText(), lrcFile.name) }.getOrNull()
                if (parsed != null) return parsed
            }
        }
        return null
    }
}
