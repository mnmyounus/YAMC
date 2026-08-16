package com.personal.filescraper

import android.app.Application
import com.personal.filescraper.di.AppContainer
import com.personal.filescraper.util.FileUtils
import com.personal.filescraper.worker.WorkerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileScraperApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Cleanup runs on its own schedule regardless of whether monitoring is
        // currently on - there can still be previously-archived files to expire.
        WorkerScheduler.scheduleCleanup(this)
        seedDefaultFoldersIfNeeded()
    }

    private fun seedDefaultFoldersIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = container.archiveRepository.getWatchedFoldersOnce()
            if (existing.isEmpty()) {
                FileUtils.defaultFolders().forEach { (path, name) ->
                    container.archiveRepository.addWatchedFolder(path, name, isDefault = true)
                }
            }
        }
    }
}
