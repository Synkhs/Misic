package com.example.core.library

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_tracks",
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["artistId"]),
        Index(value = ["folderPath"]),
        Index(value = ["title"]),
        Index(value = ["dateModified"]),
    ],
)
data class LocalTrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String,
    val folderPath: String,
    val folderName: String,
    val mimeType: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val dateModified: Long,
    val playCount: Int = 0,
    val lastPlayedMs: Long = 0L,
    val isFavorite: Boolean = false,
)

@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Entity(
    tableName = "playlist_track_cross_ref",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = LocalPlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LocalTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["playlistId", "position"]),
    ],
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
    val addedAtMs: Long,
)

data class TrackTimestampTuple(
    val id: Long,
    val dateModified: Long,
)

@Immutable
data class AlbumSummary(
    val albumId: Long,
    val album: String,
    val artist: String,
    val albumArtUri: String,
    val trackCount: Int,
)

@Immutable
data class ArtistSummary(
    val artistId: Long,
    val artist: String,
    val albumCount: Int,
    val trackCount: Int,
    val coverUri: String,
)

@Immutable
data class FolderSummary(
    val folderPath: String,
    val folderName: String,
    val trackCount: Int,
)

@Immutable
data class PlaylistWithSummary(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val coverUri: String?,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

fun LocalTrackEntity.codecLabel(): String {
    val lower = mimeType.lowercase()
    return when {
        lower.contains("flac") -> "FLAC"
        lower.contains("alac") -> "ALAC"
        lower.contains("wav") || lower.contains("x-wav") -> "WAV"
        lower.contains("ogg") || lower.contains("opus") -> "OPUS"
        lower.contains("mp4") || lower.contains("aac") -> "AAC"
        lower.contains("mpeg") || lower.contains("mp3") -> "MP3"
        else -> "AUDIO"
    }
}
