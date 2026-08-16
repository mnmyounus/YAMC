package com.personal.filescraper.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.personal.filescraper.FileScraperApp
import com.personal.filescraper.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * WorkManager re-registers its own periodic jobs after a reboot automatically, so
 * this only needs to restart the plain foreground Service if monitoring was on.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as FileScraperApp).container
                val monitoringWasOn = container.settingsRepository.monitoringEnabled.first()
                if (monitoringWasOn && PermissionUtils.hasAllFilesAccess()) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, FileMonitorService::class.java)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
