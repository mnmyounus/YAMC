package com.personal.filescraper.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "watched_folders")
data class WatchedFolderEntity(
    @PrimaryKey val path: String,
    val displayName: String,
    val isDefault: Boolean,
    val addedAtMillis: Long
)

@Dao
interface WatchedFolderDao {
    @Query("SELECT * FROM watched_folders ORDER BY isDefault DESC, displayName ASC")
    fun observeAll(): Flow<List<WatchedFolderEntity>>

    @Query("SELECT * FROM watched_folders")
    suspend fun getAllOnce(): List<WatchedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WatchedFolderEntity)

    @Query("DELETE FROM watched_folders WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("UPDATE watched_folders SET displayName = :newName WHERE path = :path")
    suspend fun updateDisplayName(path: String, newName: String)
}
