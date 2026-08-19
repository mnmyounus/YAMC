package com.personal.filescraper.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ArchivedFileEntity::class,
        WatchedFolderEntity::class,
        DismissedDefaultFolderEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun archivedFileDao(): ArchivedFileDao
    abstract fun watchedFolderDao(): WatchedFolderDao
    abstract fun dismissedDefaultFolderDao(): DismissedDefaultFolderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // A real migration, not destructive fallback - you have live archived files and
        // a customized folder list by now, and wiping the DB would orphan the actual
        // files on disk (they'd stay in Archive/ forever, untracked and never cleaned up).
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dismissed_default_folders` " +
                        "(`path` TEXT NOT NULL, PRIMARY KEY(`path`))"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "file_scraper.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
