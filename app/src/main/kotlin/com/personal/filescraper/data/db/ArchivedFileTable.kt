package com.personal.filescraper.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "archived_files",
    indices = [Index(value = ["originalPath"], unique = true)]
)
data class ArchivedFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val archivedPath: String,
    val fileName: String,
    val mimeType: String,
    val fileType: String, // FileType.name
    val sizeBytes: Long,
    val archivedAtMillis: Long,
    val sourceFolder: String
)

@Dao
interface ArchivedFileDao {
    @Query("SELECT * FROM archived_files ORDER BY archivedAtMillis DESC")
    fun observeAll(): Flow<List<ArchivedFileEntity>>

    @Query("SELECT * FROM archived_files WHERE archivedAtMillis < :cutoffMillis")
    suspend fun getExpired(cutoffMillis: Long): List<ArchivedFileEntity>

    // IGNORE on conflict: originalPath is unique, so a race between FileObserver and
    // the periodic SyncWorker detecting the same file just no-ops the second insert.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ArchivedFileEntity): Long

    @Query("DELETE FROM archived_files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM archived_files WHERE originalPath = :originalPath")
    suspend fun existsByOriginalPath(originalPath: String): Int
}
