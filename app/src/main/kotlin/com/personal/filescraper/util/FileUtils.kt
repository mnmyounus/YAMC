@file:Suppress("DEPRECATION")

package com.personal.filescraper.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.personal.filescraper.data.model.FileType
import java.io.File

object FileUtils {

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp")
    private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "mov", "avi", "3gp", "webm", "m4v")
    private val DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf")

    fun classify(file: File): FileType {
        val ext = file.extension.lowercase()
        return when {
            ext in IMAGE_EXTENSIONS -> FileType.IMAGE
            ext in VIDEO_EXTENSIONS -> FileType.VIDEO
            ext in DOCUMENT_EXTENSIONS -> FileType.DOCUMENT
            else -> FileType.OTHER
        }
    }

    fun isSupportedType(file: File): Boolean = classify(file) != FileType.OTHER

    fun isLikelyTemporary(file: File): Boolean {
        val name = file.name
        return name.startsWith(".") ||
            name.endsWith(".tmp") ||
            name.endsWith(".crdownload") ||
            name.endsWith(".part") ||
            name.endsWith(".partial")
    }

    fun mimeTypeFor(file: File): String {
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    /** Returns folders that CAN be monitored. Nothing is auto-selected anymore.
     *  User should pick from this list and save to prefs */
    fun availableFolders(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()

        // System folders
        list.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath to "DCIM")
        list.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath to "Pictures")
        list.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath to "Downloads")

        // App-specific media folders
        val mediaDir = File(Environment.getExternalStorageDirectory(), "Android/media")
        if (mediaDir.exists() && mediaDir.canRead()) {
            mediaDir.listFiles()?.filter { it.isDirectory }?.forEach { sub ->
                list.add(sub.absolutePath to sub.name)
            }
        }
        return list
    }

    fun getAppArchiveDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "Archive")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun openWithDefaultApp(context: Context, path: String) {
        try {
            val file = File(path)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeTypeFor(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("FileUtils", "Could not open $path", e)
        }
    }
}
