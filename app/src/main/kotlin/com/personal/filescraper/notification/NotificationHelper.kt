package com.personal.filescraper.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.personal.filescraper.R

class NotificationHelper(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        val monitorChannel = NotificationChannel(
            MONITOR_CHANNEL_ID, "Monitoring status", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Persistent status while folder monitoring is active" }

        val eventChannel = NotificationChannel(
            EVENT_CHANNEL_ID, "Archive activity", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "New files archived and cleanup results" }

        manager.createNotificationChannels(listOf(monitorChannel, eventChannel))
    }

    fun buildMonitoringNotification(folderCount: Int): Notification =
        NotificationCompat.Builder(context, MONITOR_CHANNEL_ID)
            .setContentTitle("Personal File Scraper")
            .setContentText(if (folderCount > 0) "Monitoring $folderCount folder(s)" else "Starting monitoring…")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    fun updateMonitoringNotification(folderCount: Int) {
        manager.notify(MONITOR_NOTIFICATION_ID, buildMonitoringNotification(folderCount))
    }

    fun showFileArchivedNotification(message: String) {
        val notification = NotificationCompat.Builder(context, EVENT_CHANNEL_ID)
            .setContentTitle("File archived")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showCleanupNotification(deletedCount: Int) {
        val notification = NotificationCompat.Builder(context, EVENT_CHANNEL_ID)
            .setContentTitle("Cleanup complete")
            .setContentText("Removed $deletedCount expired file(s)")
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()
        manager.notify(CLEANUP_NOTIFICATION_ID, notification)
    }

    fun showPermissionMissingNotification() {
        val notification = NotificationCompat.Builder(context, EVENT_CHANNEL_ID)
            .setContentTitle("Monitoring stopped")
            .setContentText("All files access was revoked. Open the app to re-grant it.")
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()
        manager.notify(PERMISSION_NOTIFICATION_ID, notification)
    }

    companion object {
        const val MONITOR_CHANNEL_ID = "monitoring_status"
        const val EVENT_CHANNEL_ID = "archive_activity"
        const val MONITOR_NOTIFICATION_ID = 1001
        const val CLEANUP_NOTIFICATION_ID = 1002
        const val PERMISSION_NOTIFICATION_ID = 1003
    }
}
