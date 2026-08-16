package com.personal.filescraper.di

import android.content.Context
import com.personal.filescraper.data.db.AppDatabase
import com.personal.filescraper.data.repository.ArchiveRepository
import com.personal.filescraper.data.repository.SettingsRepository
import com.personal.filescraper.notification.NotificationHelper

/**
 * Small hand-rolled dependency container (no Hilt) so the whole app builds without
 * an annotation-processor / DI-framework setup. Swap in Hilt later if you prefer -
 * every class here already takes its dependencies through its constructor.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)

    val settingsRepository = SettingsRepository(appContext)
    val notificationHelper = NotificationHelper(appContext)
    val archiveRepository = ArchiveRepository(appContext, database, settingsRepository)
}
