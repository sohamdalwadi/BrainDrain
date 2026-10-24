package com.dalwadi.braindrain.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "braindrain_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_DAILY_LIMIT = intPreferencesKey("daily_limit")
        private val KEY_SOFT_NUDGE_INTERVAL = intPreferencesKey("soft_nudge_interval")
        private val KEY_IS_OVERLAY_ENABLED = booleanPreferencesKey("is_overlay_enabled")
        private val KEY_IS_HAPTIC_ENABLED = booleanPreferencesKey("is_haptic_enabled")
        private val KEY_LAST_DATE = stringPreferencesKey("last_active_date")
        private val KEY_DAILY_INSTAGRAM = intPreferencesKey("daily_instagram_count")
        private val KEY_DAILY_YOUTUBE = intPreferencesKey("daily_youtube_count")
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val dailyLimitFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DAILY_LIMIT] ?: 50
    }

    val softNudgeIntervalFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOFT_NUDGE_INTERVAL] ?: 10
    }

    val isOverlayEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_OVERLAY_ENABLED] ?: true
    }

    val isHapticEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_HAPTIC_ENABLED] ?: true
    }

    val dailyInstagramCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        val today = dateFormat.format(Date())
        if (prefs[KEY_LAST_DATE] == today) prefs[KEY_DAILY_INSTAGRAM] ?: 0 else 0
    }

    val dailyYouTubeCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        val today = dateFormat.format(Date())
        if (prefs[KEY_LAST_DATE] == today) prefs[KEY_DAILY_YOUTUBE] ?: 0 else 0
    }

    suspend fun setDailyLimit(limit: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_LIMIT] = limit.coerceAtLeast(5)
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun incrementCount(platformPackage: String): Int {
        var newTotal = 0
        val today = dateFormat.format(Date())
        context.dataStore.edit { prefs ->
            val lastDate = prefs[KEY_LAST_DATE]
            if (lastDate != today) {
                prefs[KEY_LAST_DATE] = today
                prefs[KEY_DAILY_INSTAGRAM] = 0
                prefs[KEY_DAILY_YOUTUBE] = 0
            }

            when (platformPackage) {
                "com.instagram.android" -> {
                    val current = prefs[KEY_DAILY_INSTAGRAM] ?: 0
                    val updated = current + 1
                    prefs[KEY_DAILY_INSTAGRAM] = updated
                    newTotal = updated + (prefs[KEY_DAILY_YOUTUBE] ?: 0)
                }
                "com.google.android.youtube" -> {
                    val current = prefs[KEY_DAILY_YOUTUBE] ?: 0
                    val updated = current + 1
                    prefs[KEY_DAILY_YOUTUBE] = updated
                    newTotal = updated + (prefs[KEY_DAILY_INSTAGRAM] ?: 0)
                }
                else -> {
                    newTotal = (prefs[KEY_DAILY_INSTAGRAM] ?: 0) + (prefs[KEY_DAILY_YOUTUBE] ?: 0)
                }
            }
        }
        return newTotal
    }

    suspend fun resetTodayCounters() {
        val today = dateFormat.format(Date())
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_DATE] = today
            prefs[KEY_DAILY_INSTAGRAM] = 0
            prefs[KEY_DAILY_YOUTUBE] = 0
        }
    }
}
