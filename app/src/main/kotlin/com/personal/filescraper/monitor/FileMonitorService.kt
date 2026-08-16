package com.personal.filescraper.monitor

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.personal.filescraper.FileScraperApp
import com.personal.filescraper.notification.NotificationHelper
import com.personal.filescraper.util.FileUtils
import com.personal.filescraper.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class FileMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val observers = mutableListOf<RecursiveFileObserver>()
    private val container by lazy { (application as FileScraperApp).container }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH -> {
                teardownObservers()
                startMonitoring()
            }
            else -> startMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (!PermissionUtils.hasAllFilesAccess()) {
            container.notificationHelper.showPermissionMissingNotification()
            stopSelf()
            return
        }

        ServiceCompat.startForeground(
            this,
            NotificationHelper.MONITOR_NOTIFICATION_ID,
            container.notificationHelper.buildMonitoringNotification(0),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        serviceScope.launch {
            val folders = container.archiveRepository.getWatchedFoldersOnce()
            var watchedCount = 0
            folders.forEach { folder ->
                // One folder failing to watch (bad permissions, huge tree hitting the
                // inotify cap, etc.) must never take the rest of monitoring down with it.
                try {
                    val observer = RecursiveFileObserver(folder.path) { file ->
                        serviceScope.launch { handleNewFile(file, folder.path) }
                    }
                    observer.startWatching()
                    observers.add(observer)
                    watchedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to watch folder ${folder.path}", e)
                }
            }
            container.notificationHelper.updateMonitoringNotification(watchedCount)
        }
    }

    private suspend fun handleNewFile(file: File, sourceFolder: String) {
        try {
            if (!file.exists() || !FileUtils.isSupportedType(file)) return
            val archived = container.archiveRepository.archiveFile(file, sourceFolder) ?: return
            container.notificationHelper.showFileArchivedNotification(archived.fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle new file ${file.path}", e)
        }
    }

    private fun teardownObservers() {
        observers.forEach { it.stopWatching() }
        observers.clear()
    }

    override fun onDestroy() {
        teardownObservers()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "FileMonitorService"
        const val ACTION_STOP = "com.personal.filescraper.action.STOP"
        const val ACTION_REFRESH = "com.personal.filescraper.action.REFRESH"
    }
}
