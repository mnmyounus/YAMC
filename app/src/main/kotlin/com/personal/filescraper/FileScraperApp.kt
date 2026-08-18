package com.personal.filescraper

import android.app.Application
import com.personal.filescraper.di.AppContainer
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
        WorkerScheduler.scheduleCleanup(this)
        // Runs on every launch, not just the first - see
        // ArchiveRepository.syncDefaultFolders for why a one-time-only seed misses
        // folders (like WhatsApp's) that didn't exist yet at first install.
        CoroutineScope(Dispatchers.IO).launch {
            container.archiveRepository.syncDefaultFolders()
        }
    }
}
