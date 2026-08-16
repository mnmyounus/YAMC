package com.personal.filescraper.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.personal.filescraper.FileScraperApp

class CleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as FileScraperApp).container
        val deletedCount = container.archiveRepository.deleteExpiredFiles()
        if (deletedCount > 0) {
            container.notificationHelper.showCleanupNotification(deletedCount)
        }
        return Result.success()
    }
}
