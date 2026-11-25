package com.example.core.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LocalTrackEntity::class,
        LocalPlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        TrackBookmarkEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MagicLibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile
        private var INSTANCE: MagicLibraryDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_tracks ADD COLUMN artworkStatus TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_tracks_playCount ON local_tracks(playCount)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_tracks_isFavorite ON local_tracks(isFavorite)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS track_bookmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trackKey TEXT NOT NULL,
                        positionMs INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        createdAtMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_track_bookmarks_trackKey ON track_bookmarks(trackKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_track_bookmarks_createdAtMs ON track_bookmarks(createdAtMs)")
            }
        }

        fun getInstance(context: Context): MagicLibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MagicLibraryDatabase::class.java,
                    "magic_music_library.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
