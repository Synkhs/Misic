package com.example.core.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LibraryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = MagicLibraryDatabase.getInstance(appContext).libraryDao()

    fun hasAudioPermission(): Boolean = MediaStoreScanner.hasAudioPermission(appContext)

    fun observeTrackCount(): Flow<Int> = dao.observeTrackCount()

    fun observeRecentlyPlayed(limit: Int = 12): Flow<List<LocalTrackEntity>> =
        dao.observeRecentlyPlayedTracks(limit)

    fun observePlaylists(): Flow<List<PlaylistWithSummary>> = dao.observePlaylists()

    fun observePlaylistTracks(playlistId: Long): Flow<List<LocalTrackEntity>> =
        dao.observePlaylistTracks(playlistId)

    fun observeAlbums(query: String): Flow<List<AlbumSummary>> = dao.observeAlbums(query)

    fun observeArtists(query: String): Flow<List<ArtistSummary>> = dao.observeArtists(query)

    fun observeFolders(query: String): Flow<List<FolderSummary>> = dao.observeFolders(query)

    fun pagedTracks(query: String): Flow<PagingData<LocalTrackEntity>> = Pager(
        config = PagingConfig(pageSize = 30, enablePlaceholders = false),
        pagingSourceFactory = { dao.pagingAllTracks(query) },
    ).flow

    fun pagedTracksByAlbum(albumId: Long): Flow<PagingData<LocalTrackEntity>> = Pager(
        config = PagingConfig(pageSize = 30, enablePlaceholders = false),
        pagingSourceFactory = { dao.pagingTracksByAlbum(albumId) },
    ).flow

    fun pagedTracksByArtist(artistId: Long): Flow<PagingData<LocalTrackEntity>> = Pager(
        config = PagingConfig(pageSize = 30, enablePlaceholders = false),
        pagingSourceFactory = { dao.pagingTracksByArtist(artistId) },
    ).flow

    fun pagedTracksByFolder(folderPath: String): Flow<PagingData<LocalTrackEntity>> = Pager(
        config = PagingConfig(pageSize = 30, enablePlaceholders = false),
        pagingSourceFactory = { dao.pagingTracksByFolder(folderPath) },
    ).flow

    suspend fun syncMediaStoreIncremental(): Int = withContext(Dispatchers.IO) {
        if (!MediaStoreScanner.hasAudioPermission(appContext)) return@withContext 0
        val scanned = MediaStoreScanner.scanDeviceAudio(appContext)
        val existingMap = dao.getTrackTimestamps().associate { it.id to it.dateModified }
        val scannedIds = scanned.map { it.id }.toSet()

        val deletedIds = existingMap.keys.filterNot { scannedIds.contains(it) }
        if (deletedIds.isNotEmpty()) {
            deletedIds.chunked(200).forEach { chunk ->
                dao.deleteTracksByIds(chunk)
            }
        }

        val toUpsert = scanned.filter { track ->
            val oldMod = existingMap[track.id]
            oldMod == null || oldMod != track.dateModified
        }
        if (toUpsert.isNotEmpty()) {
            toUpsert.chunked(200).forEach { chunk ->
                dao.upsertTracks(chunk)
            }
        }
        scanned.size
    }

    suspend fun getAllTracksSnapshot(): List<LocalTrackEntity> = withContext(Dispatchers.IO) {
        dao.getAllTracksSnapshot()
    }

    suspend fun getAlbumTracksSnapshot(albumId: Long): List<LocalTrackEntity> = withContext(Dispatchers.IO) {
        dao.getTracksByAlbumSnapshot(albumId)
    }

    suspend fun getArtistTracksSnapshot(artistId: Long): List<LocalTrackEntity> = withContext(Dispatchers.IO) {
        dao.getTracksByArtistSnapshot(artistId)
    }

    suspend fun getFolderTracksSnapshot(folderPath: String): List<LocalTrackEntity> = withContext(Dispatchers.IO) {
        dao.getTracksByFolderSnapshot(folderPath)
    }

    suspend fun getPlaylistTracksSnapshot(playlistId: Long): List<LocalTrackEntity> = withContext(Dispatchers.IO) {
        dao.getPlaylistTracksSnapshot(playlistId)
    }

    suspend fun searchTracksSnapshot(query: String, limit: Int = 15): List<LocalTrackEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) emptyList() else dao.searchTracksSnapshot(query, limit)
    }

    suspend fun findArtistIdByName(artistName: String): Long? = withContext(Dispatchers.IO) {
        dao.findFirstTrackByArtistName(artistName)?.artistId
    }

    suspend fun findAlbumIdByTrack(title: String, artist: String): Long? = withContext(Dispatchers.IO) {
        dao.searchTracksSnapshot(title, 5).firstOrNull {
            it.artist.equals(artist, ignoreCase = true) || artist.contains(it.artist, ignoreCase = true)
        }?.albumId
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.insertPlaylist(
            LocalPlaylistEntity(
                name = name.trim().ifBlank { "Nueva lista" },
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        val clean = newName.trim()
        if (clean.isNotEmpty()) {
            dao.renamePlaylist(playlistId, clean, System.currentTimeMillis())
        }
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        val pos = dao.nextPlaylistPosition(playlistId)
        dao.insertPlaylistTrack(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = pos,
                addedAtMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylistTrack(playlistId, trackId)
    }

    suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<Long>) = withContext(Dispatchers.IO) {
        dao.updatePlaylistOrder(playlistId, orderedTrackIds)
    }

    suspend fun recordLocalTrackPlay(trackId: Long) = withContext(Dispatchers.IO) {
        dao.incrementPlayCount(trackId, System.currentTimeMillis())
    }

    suspend fun setTrackFavorite(trackId: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        dao.setTrackFavorite(trackId, favorite)
    }

    fun buildShareTrackIntent(track: LocalTrackEntity): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = track.mimeType.ifBlank { "audio/*" }
            putExtra(Intent.EXTRA_STREAM, Uri.parse(track.contentUri))
            putExtra(Intent.EXTRA_SUBJECT, track.title)
            putExtra(Intent.EXTRA_TEXT, "${track.title} — ${track.artist}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
