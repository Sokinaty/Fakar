package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore by preferencesDataStore(name = "fekr_reminder_prefs")

data class ReminderSettings(
    val enabled: Boolean = true,
    val leadMinutes: Int = 120,
    val includeHomePrepTip: Boolean = true
)

class ReminderPreferences(private val context: Context) {
    private val keyEnabled = booleanPreferencesKey("reminders_enabled")
    private val keyLeadMinutes = intPreferencesKey("lead_minutes")
    private val keyPrepTip = booleanPreferencesKey("include_prep_tip")

    val settingsFlow: Flow<ReminderSettings> = context.reminderDataStore.data.map { prefs ->
        ReminderSettings(
            enabled = prefs[keyEnabled] ?: true,
            leadMinutes = prefs[keyLeadMinutes] ?: 120,
            includeHomePrepTip = prefs[keyPrepTip] ?: true
        )
    }

    suspend fun updateSettings(enabled: Boolean, leadMinutes: Int, includePrepTip: Boolean) {
        context.reminderDataStore.edit { prefs ->
            prefs[keyEnabled] = enabled
            prefs[keyLeadMinutes] = leadMinutes
            prefs[keyPrepTip] = includePrepTip
        }
    }
}
