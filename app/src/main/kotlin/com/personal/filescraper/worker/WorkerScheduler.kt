package com.personal.filescraper.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkerScheduler {
    private const val CLEANUP_WORK_NAME = "cleanup_worker"
    private const val SYNC_WORK_NAME = "sync_worker"

    fun scheduleCleanup(context: Context) {
        val request = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(CLEANUP_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun scheduleSync(context: Context) {
        // 15 minutes is WorkManager's minimum period for periodic work.
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(SYNC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
    }
}
