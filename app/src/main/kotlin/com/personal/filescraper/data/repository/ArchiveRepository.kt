package com.personal.filescraper.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.personal.filescraper.data.db.AppDatabase
import com.personal.filescraper.data.db.ArchivedFileEntity
import com.personal.filescraper.data.db.WatchedFolderEntity
import com.personal.filescraper.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class ArchiveRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    // Resolves to /Android/data/com.personal.filescraper/files/Archive/
    private val archiveDir: File by lazy {
        File(context.getExternalFilesDir(null), "Archive").apply { mkdirs() }
    }

    fun observeWatchedFolders(): Flow<List<WatchedFolderEntity>> = db.watchedFolderDao().observeAll()
    suspend fun getWatchedFoldersOnce(): List<WatchedFolderEntity> = db.watchedFolderDao().getAllOnce()

    suspend fun addWatchedFolder(path: String, displayName: String, isDefault: Boolean = false) {
        db.watchedFolderDao().insert(
            WatchedFolderEntity(
                path = path,
                displayName = displayName,
                isDefault = isDefault,
                addedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeWatchedFolder(path: String) = db.watchedFolderDao().deleteByPath(path)

    fun observeArchivedFiles(): Flow<List<ArchivedFileEntity>> = db.archivedFileDao().observeAll()

    suspend fun isAlreadyArchived(originalPath: String): Boolean =
        db.archivedFileDao().existsByOriginalPath(originalPath) > 0

    suspend fun archiveFile(sourceFile: File, sourceFolder: String): ArchivedFileEntity? =
        withContext(Dispatchers.IO) {
            try {
                if (FileUtils.isLikelyTemporary(sourceFile)) return@withContext null
                if (isAlreadyArchived(sourceFile.absolutePath)) return@withContext null

                val destName = uniqueDestName(sourceFile.name)
                val destFile = File(archiveDir, destName)
                sourceFile.inputStream().use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                // Raw byte copy already preserves embedded metadata (EXIF, etc.) since
                // nothing re-encodes the file. This preserves the filesystem-level
                // last-modified timestamp too, which a fresh output stream otherwise resets.
                destFile.setLastModified(sourceFile.lastModified())

                val entity = ArchivedFileEntity(
                    originalPath = sourceFile.absolutePath,
                    archivedPath = destFile.absolutePath,
                    fileName = destName,
                    mimeType = FileUtils.mimeTypeFor(sourceFile),
                    fileType = FileUtils.classify(sourceFile).name,
                    sizeBytes = destFile.length(),
                    archivedAtMillis = System.currentTimeMillis(),
                    sourceFolder = sourceFolder
                )
                val id = db.archivedFileDao().insert(entity)
                if (id == -1L) {
                    // Lost a race with another detection path (FileObserver vs SyncWorker).
                    destFile.delete()
                    return@withContext null
                }
                entity.copy(id = id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to archive ${sourceFile.path}", e)
                null
            }
        }

    private fun uniqueDestName(originalName: String): String {
        var candidate = originalName
        var counter = 1
        while (File(archiveDir, candidate).exists()) {
            val dot = originalName.lastIndexOf('.')
            candidate = if (dot >= 0) {
                "${originalName.substring(0, dot)}_$counter${originalName.substring(dot)}"
            } else {
                "${originalName}_$counter"
            }
            counter++
        }
        return candidate
    }

    suspend fun deleteExpiredFiles(): Int = withContext(Dispatchers.IO) {
        val retentionHours = settingsRepository.retentionHours.first()
        val retentionMillis = retentionHours * 60L * 60L * 1000L
        val cutoff = System.currentTimeMillis() - retentionMillis
        val expired = db.archivedFileDao().getExpired(cutoff)
        expired.forEach { entity ->
            val file = File(entity.archivedPath)
            if (file.exists()) file.delete()
            db.archivedFileDao().deleteById(entity.id)
        }
        expired.size
    }

    /**
     * Copies every currently-archived file into a user-chosen folder (picked via the
     * system folder picker), so files can be kept permanently instead of being caught
     * by the retention cleanup. This only copies out - it never touches the internal
     * archive or the cleanup timer, so anything not exported in time still expires
     * on schedule as normal.
     */
    suspend fun exportAll(context: Context, destinationTreeUri: Uri): Int = withContext(Dispatchers.IO) {
        val destTree = DocumentFile.fromTreeUri(context, destinationTreeUri) ?: return@withContext 0
        val allFiles = db.archivedFileDao().observeAll().first()
        var exported = 0
        allFiles.forEach { entity ->
            try {
                val sourceFile = File(entity.archivedPath)
                if (!sourceFile.exists()) return@forEach
                if (destTree.findFile(entity.fileName) != null) return@forEach // already exported

                val newDoc = destTree.createFile(entity.mimeType, entity.fileName) ?: return@forEach
                context.contentResolver.openOutputStream(newDoc.uri)?.use { output ->
                    sourceFile.inputStream().use { input -> input.copyTo(output) }
                }
                exported++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export ${entity.fileName}", e)
            }
        }
        exported
    }

    companion object {
        private const val TAG = "ArchiveRepository"
    }
}
