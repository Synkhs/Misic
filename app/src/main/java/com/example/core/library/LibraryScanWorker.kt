package com.example.core.library

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.core.artwork.LocalArtworkPipeline
import com.example.core.artwork.LocalArtworkRequest
import com.example.core.artwork.LocalArtworkSourceKind

class LibraryScanWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val repo = LibraryRepository(applicationContext)
            if (!repo.hasAudioPermission()) return@runCatching Result.success()
            repo.syncMediaStoreIncremental()
            val tracks = repo.getAllTracksSnapshot()
            // Pre-warm one track thumbnail per unique album (capped to 32) on IO without blocking startup
            tracks.distinctBy { it.albumId }
                .take(32)
                .forEach { track ->
                    val req = LocalArtworkRequest(
                        trackId = track.id,
                        albumId = track.albumId,
                        contentUri = track.contentUri,
                        albumArtUri = track.albumArtUri,
                        dateModifiedSec = track.dateModified,
                        targetSizePx = 128,
                    )
                    val (_, sourceKind) = LocalArtworkPipeline.resolveAndCacheBytes(applicationContext, req)
                    val status = when (sourceKind) {
                        LocalArtworkSourceKind.DISK_CACHE,
                        LocalArtworkSourceKind.LOAD_THUMBNAIL -> "CACHED"
                        LocalArtworkSourceKind.EMBEDDED_METADATA -> "EMBEDDED"
                        LocalArtworkSourceKind.FOLDER_ART -> "FOLDER"
                        LocalArtworkSourceKind.LEGACY_ALBUM_ART_URI -> "CACHED"
                        LocalArtworkSourceKind.NONE -> "NONE"
                    }
                    repo.updateTrackArtworkStatus(track.id, status)
                }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "magic_library_incremental_scan"

        fun enqueueIncremental(context: Context) {
            runCatching {
                val request = OneTimeWorkRequestBuilder<LibraryScanWorker>().build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    request,
                )
            }
        }
    }
}
