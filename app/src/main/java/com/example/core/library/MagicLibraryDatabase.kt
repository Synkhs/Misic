package com.example.core.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocalTrackEntity::class,
        LocalPlaylistEntity::class,
        PlaylistTrackCrossRef::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MagicLibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile
        private var INSTANCE: MagicLibraryDatabase? = null

        fun getInstance(context: Context): MagicLibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MagicLibraryDatabase::class.java,
                    "magic_music_library.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
