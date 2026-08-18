package com.personal.filescraper.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.personal.filescraper.data.db.AppDatabase
import com.personal.filescraper.data.db.ArchivedFileEntity
import com.personal.filescraper.data.db.DismissedDefaultFolderEntity
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

    suspend fun renameWatchedFolder(path: String, newDisplayName: String) =
        db.watchedFolderDao().updateDisplayName(path, newDisplayName)

    suspend fun dismissDefaultFolder(path: String) =
        db.dismissedDefaultFolderDao().insert(DismissedDefaultFolderEntity(path))

}

}

    suspend fun syncDefaultFolders() {
        val existingPaths = getWatchedFoldersOnce().map { it.path }.toSet()
        val dismissedPaths = db.dismissedDefaultFolderDao().getAllPaths().toSet()
        FileUtils.defaultFolders().forEach { (path, name) ->
            if (path !in existingPaths && path !in dismissedPaths) {
                addWatchedFolder(path, name, isDefault = true)
            }
        }
    }

    fun observeArchivedFiles(): Flow<List<ArchivedFileEntity>> = db.rchivedFileDao().observeAll()

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

    suspend fun exportAll(context: Context, destinationTreeUri: Uri): Int = withContext(Dispatchers.IO) {
        val destTree = DocumentFile.fromTreeUri(context, destinationTreeUri) ?: return@withContext 0
        val allFiles = db.archivedFileDao().observeAll().first()
        var exported = 0
        allFiles.forEach { entity ->
            try {
                val sourceFile = File(entity.archivedPath)
                if (!sourceFile.exists()) return@forEach
                if (destTree.findFile(entity.fileName) != null) return@forEach

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
