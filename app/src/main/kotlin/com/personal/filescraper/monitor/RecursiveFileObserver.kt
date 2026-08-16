package com.personal.filescraper.monitor

import android.os.FileObserver
import android.util.Log
import java.io.File

/**
 * Watches a directory tree for new files.
 *
 * [FileObserver] only watches a single directory, not its subtree, so this class
 * creates one child observer per directory and adds new ones on the fly whenever a
 * subdirectory is created. Files are reported on CLOSE_WRITE or MOVED_TO - never on
 * raw CREATE - because CREATE can fire before a file's bytes actually exist (many
 * camera and download apps create the file first and write its contents afterwards,
 * so acting on CREATE risks archiving an empty or partial file).
 */
class RecursiveFileObserver(
    rootPath: String,
    private val onFileReady: (File) -> Unit
) {
    private val rootFile = File(rootPath)
    private val watchMask = FileObserver.CREATE or FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO
    private val observers = mutableMapOf<String, FileObserver>()

    @Volatile private var active = false

    fun startWatching() {
        if (active) return
        active = true
        if (!rootFile.exists() || !rootFile.isDirectory) {
            Log.w(TAG, "Not a directory, skipping: ${rootFile.absolutePath}")
            return
        }
        watchRecursively(rootFile, depth = 0)
    }

    fun stopWatching() {
        active = false
        observers.values.forEach { it.stopWatching() }
        observers.clear()
    }

    private fun watchRecursively(dir: File, depth: Int) {
        if (depth > MAX_DEPTH || observers.containsKey(dir.absolutePath)) return

        val observer = object : FileObserver(dir, watchMask) {
            override fun onEvent(event: Int, relativePath: String?) {
                if (relativePath == null || !active) return
                val child = File(dir, relativePath)
                when (event and ALL_EVENTS) {
                    CREATE -> if (child.isDirectory) watchRecursively(child, depth + 1)
                    CLOSE_WRITE -> if (child.isFile) onFileReady(child)
                    MOVED_TO -> {
                        if (child.isFile) onFileReady(child)
                        else if (child.isDirectory) watchRecursively(child, depth + 1)
                    }
                }
            }
        }
        observer.startWatching()
        observers[dir.absolutePath] = observer

        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) watchRecursively(child, depth + 1)
        }
    }

    companion object {
        private const val TAG = "RecursiveFileObserver"
        private const val MAX_DEPTH = 12
    }
}
