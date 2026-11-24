package com.example.core.library

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query(
        """
        SELECT * FROM local_tracks
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%')
        ORDER BY title COLLATE NOCASE ASC
        """,
    )
    fun pagingAllTracks(query: String): PagingSource<Int, LocalTrackEntity>

    @Query("SELECT * FROM local_tracks WHERE albumId = :albumId ORDER BY id ASC")
    fun pagingTracksByAlbum(albumId: Long): PagingSource<Int, LocalTrackEntity>

    @Query("SELECT * FROM local_tracks WHERE artistId = :artistId ORDER BY playCount DESC, title COLLATE NOCASE ASC")
    fun pagingTracksByArtist(artistId: Long): PagingSource<Int, LocalTrackEntity>

    @Query("SELECT * FROM local_tracks WHERE folderPath = :folderPath ORDER BY title COLLATE NOCASE ASC")
    fun pagingTracksByFolder(folderPath: String): PagingSource<Int, LocalTrackEntity>

    @Query("SELECT * FROM local_tracks WHERE lastPlayedMs > 0 ORDER BY lastPlayedMs DESC LIMIT :limit")
    fun observeRecentlyPlayedTracks(limit: Int = 12): Flow<List<LocalTrackEntity>>

    @Query("SELECT COUNT(*) FROM local_tracks")
    fun observeTrackCount(): Flow<Int>

    @Query("SELECT * FROM local_tracks ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllTracksSnapshot(): List<LocalTrackEntity>

    @Query("SELECT * FROM local_tracks WHERE albumId = :albumId ORDER BY id ASC")
    suspend fun getTracksByAlbumSnapshot(albumId: Long): List<LocalTrackEntity>

    @Query("SELECT * FROM local_tracks WHERE artistId = :artistId ORDER BY playCount DESC, title COLLATE NOCASE ASC")
    suspend fun getTracksByArtistSnapshot(artistId: Long): List<LocalTrackEntity>

    @Query("SELECT * FROM local_tracks WHERE folderPath = :folderPath ORDER BY title COLLATE NOCASE ASC")
    suspend fun getTracksByFolderSnapshot(folderPath: String): List<LocalTrackEntity>

    @Query(
        """
        SELECT * FROM local_tracks
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%')
        ORDER BY title COLLATE NOCASE ASC
        LIMIT :limit
        """,
    )
    suspend fun searchTracksSnapshot(query: String, limit: Int = 20): List<LocalTrackEntity>

    @Query("SELECT * FROM local_tracks WHERE artist LIKE '%' || :artistName || '%' ORDER BY playCount DESC, title COLLATE NOCASE ASC LIMIT 1")
    suspend fun findFirstTrackByArtistName(artistName: String): LocalTrackEntity?

    @Query("SELECT id, dateModified FROM local_tracks")
    suspend fun getTrackTimestamps(): List<TrackTimestampTuple>

    @Upsert
    suspend fun upsertTracks(tracks: List<LocalTrackEntity>)

    @Query("DELETE FROM local_tracks WHERE id IN (:ids)")
    suspend fun deleteTracksByIds(ids: List<Long>)

    @Query("UPDATE local_tracks SET playCount = playCount + 1, lastPlayedMs = :nowMs WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: Long, nowMs: Long)

    @Query("UPDATE local_tracks SET isFavorite = :favorite WHERE id = :trackId")
    suspend fun setTrackFavorite(trackId: Long, favorite: Boolean)

    @Query(
        """
        SELECT albumId, album, artist, MAX(albumArtUri) AS albumArtUri, COUNT(*) AS trackCount
        FROM local_tracks
        WHERE (:query = '' OR album LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%')
        GROUP BY albumId
        ORDER BY album COLLATE NOCASE ASC
        """,
    )
    fun observeAlbums(query: String): Flow<List<AlbumSummary>>

    @Query(
        """
        SELECT artistId, artist, COUNT(DISTINCT albumId) AS albumCount, COUNT(*) AS trackCount, MAX(albumArtUri) AS coverUri
        FROM local_tracks
        WHERE (:query = '' OR artist LIKE '%' || :query || '%')
        GROUP BY artistId
        ORDER BY artist COLLATE NOCASE ASC
        """,
    )
    fun observeArtists(query: String): Flow<List<ArtistSummary>>

    @Query(
        """
        SELECT folderPath, folderName, COUNT(*) AS trackCount
        FROM local_tracks
        WHERE (:query = '' OR folderName LIKE '%' || :query || '%' OR folderPath LIKE '%' || :query || '%')
        GROUP BY folderPath
        ORDER BY folderName COLLATE NOCASE ASC
        """,
    )
    fun observeFolders(query: String): Flow<List<FolderSummary>>

    @Query(
        """
        SELECT p.id, p.name, COUNT(r.trackId) AS trackCount, MAX(t.albumArtUri) AS coverUri, p.createdAtMs, p.updatedAtMs
        FROM local_playlists p
        LEFT JOIN playlist_track_cross_ref r ON p.id = r.playlistId
        LEFT JOIN local_tracks t ON r.trackId = t.id
        GROUP BY p.id
        ORDER BY p.updatedAtMs DESC
        """,
    )
    fun observePlaylists(): Flow<List<PlaylistWithSummary>>

    @Query(
        """
        SELECT t.* FROM local_tracks t
        INNER JOIN playlist_track_cross_ref r ON t.id = r.trackId
        WHERE r.playlistId = :playlistId
        ORDER BY r.position ASC
        """,
    )
    fun observePlaylistTracks(playlistId: Long): Flow<List<LocalTrackEntity>>

    @Query(
        """
        SELECT t.* FROM local_tracks t
        INNER JOIN playlist_track_cross_ref r ON t.id = r.trackId
        WHERE r.playlistId = :playlistId
        ORDER BY r.position ASC
        """,
    )
    suspend fun getPlaylistTracksSnapshot(playlistId: Long): List<LocalTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: LocalPlaylistEntity): Long

    @Query("UPDATE local_playlists SET name = :newName, updatedAtMs = :nowMs WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String, nowMs: Long)

    @Query("DELETE FROM local_playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun nextPlaylistPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrack(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deletePlaylistTrack(playlistId: Long, trackId: Long)

    @Query("UPDATE playlist_track_cross_ref SET position = :position WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun updatePlaylistTrackPosition(playlistId: Long, trackId: Long, position: Int)

    @Transaction
    suspend fun updatePlaylistOrder(playlistId: Long, orderedTrackIds: List<Long>) {
        orderedTrackIds.forEachIndexed { idx, trackId ->
            updatePlaylistTrackPosition(playlistId, trackId, idx)
        }
    }
}
