package com.personal.filescraper.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "file_scraper_settings")

class SettingsRepository(private val context: Context) {

    val monitoringEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[MONITORING_ENABLED] ?: false }

    val retentionHours: Flow<Int> =
        context.dataStore.data.map { it[RETENTION_HOURS] ?: DEFAULT_RETENTION_HOURS }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { it[MONITORING_ENABLED] = enabled }
    }

    suspend fun setRetentionHours(hours: Int) {
        context.dataStore.edit { it[RETENTION_HOURS] = hours }
    }

    companion object {
        const val DEFAULT_RETENTION_HOURS = 48
        private val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        private val RETENTION_HOURS = intPreferencesKey("retention_hours")
    }
}
