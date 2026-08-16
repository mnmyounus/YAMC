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
 * raw CREATE - because CREATE can fire before a file's bytes actually exist.
 *
 * Two defensive limits keep one huge folder tree (e.g. a messaging app's media folder,
 * which can easily have dozens of subfolders on its own) from destabilizing the app:
 *  - MAX_DEPTH stops recursing past a sane nesting level.
 *  - MAX_WATCHED_DIRS stops creating new inotify watches past a per-root cap, since the
 *    OS caps how many watches a process can hold in total. Hitting either limit doesn't
 *    lose files permanently - SyncWorker's periodic re-scan still covers anything past
 *    the cap, just with some delay instead of instantly.
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
        try {
            watchRecursively(rootFile, depth = 0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start watching ${rootFile.absolutePath}", e)
        }
    }

    fun stopWatching() {
        active = false
        observers.values.forEach {
            try {
                it.stopWatching()
            } catch (e: Exception) {
                // Already torn down - nothing to do.
            }
        }
        observers.clear()
    }

    private fun watchRecursively(dir: File, depth: Int) {
        if (!active) return
        if (depth > MAX_DEPTH) return
        if (observers.size >= MAX_WATCHED_DIRS) return
        if (observers.containsKey(dir.absolutePath)) return

        try {
            val observer = object : FileObserver(dir, watchMask) {
                override fun onEvent(event: Int, relativePath: String?) {
                    if (relativePath == null || !active) return
                    try {
                        val child = File(dir, relativePath)
                        when (event and ALL_EVENTS) {
                            CREATE -> if (child.isDirectory) watchRecursively(child, depth + 1)
                            CLOSE_WRITE -> if (child.isFile) onFileReady(child)
                            MOVED_TO -> {
                                if (child.isFile) onFileReady(child)
                                else if (child.isDirectory) watchRecursively(child, depth + 1)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling event in ${dir.absolutePath}", e)
                    }
                }
            }
            observer.startWatching()
            observers[dir.absolutePath] = observer
        } catch (e: Exception) {
            // Most likely cause: the process-wide inotify watch limit was hit. Skip this
            // one directory instead of crashing - SyncWorker still covers it periodically.
            Log.w(TAG, "Could not watch ${dir.absolutePath}, skipping", e)
            return
        }

        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) watchRecursively(child, depth + 1)
        }
    }

    companion object {
        private const val TAG = "RecursiveFileObserver"
        private const val MAX_DEPTH = 10
        private const val MAX_WATCHED_DIRS = 500
    }
}
