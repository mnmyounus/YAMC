package com.personal.filescraper.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Tracks default folders the user explicitly chose to stop watching (via swipe-to-
 * remove), so the periodic default-folder sync doesn't keep re-adding them. Without
 * this, removing a default folder to fix lag would just have it silently reappear.
 */
@Entity(tableName = "dismissed_default_folders")
data class DismissedDefaultFolderEntity(
    @PrimaryKey val path: String
)

@Dao
interface DismissedDefaultFolderDao {
    @Query("SELECT path FROM dismissed_default_folders")
    suspend fun getAllPaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DismissedDefaultFolderEntity)
}
