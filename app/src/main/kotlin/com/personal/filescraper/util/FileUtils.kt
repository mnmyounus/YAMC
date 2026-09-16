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
    private val TEXT_EXTENSIONS = setOf("txt", "csv")

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

    // Formats with a genuine built-in viewer in this app.
    fun isPdf(file: File): Boolean = file.extension.lowercase() == "pdf"
    fun isPlainText(file: File): Boolean = file.extension.lowercase() in TEXT_EXTENSIONS

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

    fun defaultFolders(): List<Pair<String, String>> {
        val root = Environment.getExternalStorageDirectory()
        val list = mutableListOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath to "DCIM",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath to "Pictures",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath to "Downloads"
        )

        val mediaDir = File(root, "Android/media")
        mediaDir.listFiles()?.filter { it.isDirectory }?.forEach { sub ->
            list.add(sub.absolutePath to sub.name)
        }

        val legacyWhatsAppMedia = File(root, "WhatsApp/Media")
        if (legacyWhatsAppMedia.isDirectory) {
            list.add(legacyWhatsAppMedia.absolutePath to "WhatsApp (legacy path)")
        }

        return list
    }

    // Fallback for formats without a built-in viewer (doc/docx/xls/xlsx/ppt/pptx/rtf).
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

    fun shareFile(context: Context, path: String) {
        try {
            val file = File(path)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeTypeFor(file)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share"))
        } catch (e: Exception) {
            Log.e("FileUtils", "Could not share $path", e)
        }
    }
}
