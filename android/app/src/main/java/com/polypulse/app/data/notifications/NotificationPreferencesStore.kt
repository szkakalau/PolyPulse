package com.polypulse.app.data.notifications

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "notification_prefs")

data class NotificationPreferences(
    val whaleRadarEnabled: Boolean,
    val dailyPulseEnabled: Boolean,
    val quietHoursEnabled: Boolean,
    val quietHoursStart: Int,
    val quietHoursEnd: Int
)

data class NotificationThrottleConfig(
    val whaleAlertIntervalMinutes: Int,
    val whalePushIntervalMinutes: Int,
    val dailyPushIntervalMinutes: Int,
    val generalPushIntervalMinutes: Int
)

class NotificationPreferencesStore(private val context: Context) {
    companion object {
        private val WHALE_RADAR_ENABLED = booleanPreferencesKey("whale_radar_enabled")
        private val DAILY_PULSE_ENABLED = booleanPreferencesKey("daily_pulse_enabled")
        private val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        private val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        private val LAST_ALERT_SENT_AT = longPreferencesKey("last_alert_sent_at")
        private val LAST_WHALE_ALERT_SENT_AT = longPreferencesKey("last_whale_alert_sent_at")
        private val LAST_PUSH_SENT_AT = longPreferencesKey("last_push_sent_at")
        private val LAST_WHALE_PUSH_SENT_AT = longPreferencesKey("last_whale_push_sent_at")
        private val LAST_DAILY_PUSH_SENT_AT = longPreferencesKey("last_daily_push_sent_at")
        private val LAST_GENERAL_PUSH_SENT_AT = longPreferencesKey("last_general_push_sent_at")
        private val WHALE_ALERT_INTERVAL_MIN = intPreferencesKey("whale_alert_interval_min")
        private val WHALE_PUSH_INTERVAL_MIN = intPreferencesKey("whale_push_interval_min")
        private val DAILY_PUSH_INTERVAL_MIN = intPreferencesKey("daily_push_interval_min")
        private val GENERAL_PUSH_INTERVAL_MIN = intPreferencesKey("general_push_interval_min")
    }

    val preferences: Flow<NotificationPreferences> = context.dataStore.data.map { prefs ->
        NotificationPreferences(
            whaleRadarEnabled = prefs[WHALE_RADAR_ENABLED] ?: true,
            dailyPulseEnabled = prefs[DAILY_PULSE_ENABLED] ?: false,
            quietHoursEnabled = prefs[QUIET_HOURS_ENABLED] ?: false,
            quietHoursStart = prefs[QUIET_HOURS_START] ?: 22,
            quietHoursEnd = prefs[QUIET_HOURS_END] ?: 7
        )
    }

    suspend fun getSnapshot(): NotificationPreferences {
        return preferences.first()
    }

    suspend fun setWhaleRadarEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[WHALE_RADAR_ENABLED] = enabled
        }
    }

    suspend fun setDailyPulseEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DAILY_PULSE_ENABLED] = enabled
        }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[QUIET_HOURS_ENABLED] = enabled
        }
    }

    suspend fun setQuietHours(startHour: Int, endHour: Int) {
        context.dataStore.edit { prefs ->
            prefs[QUIET_HOURS_START] = startHour
            prefs[QUIET_HOURS_END] = endHour
        }
    }

    suspend fun getThrottleConfig(): NotificationThrottleConfig {
        val prefs = context.dataStore.data.first()
        return NotificationThrottleConfig(
            whaleAlertIntervalMinutes = prefs[WHALE_ALERT_INTERVAL_MIN] ?: 2,
            whalePushIntervalMinutes = prefs[WHALE_PUSH_INTERVAL_MIN] ?: 1,
            dailyPushIntervalMinutes = prefs[DAILY_PUSH_INTERVAL_MIN] ?: 360,
            generalPushIntervalMinutes = prefs[GENERAL_PUSH_INTERVAL_MIN] ?: 1
        )
    }

    suspend fun setThrottleConfig(config: NotificationThrottleConfig) {
        context.dataStore.edit { prefs ->
            prefs[WHALE_ALERT_INTERVAL_MIN] = config.whaleAlertIntervalMinutes
            prefs[WHALE_PUSH_INTERVAL_MIN] = config.whalePushIntervalMinutes
            prefs[DAILY_PUSH_INTERVAL_MIN] = config.dailyPushIntervalMinutes
            prefs[GENERAL_PUSH_INTERVAL_MIN] = config.generalPushIntervalMinutes
        }
    }

    suspend fun shouldThrottleAlert(intervalMs: Long): Boolean {
        return shouldThrottleWhaleAlert(intervalMs)
    }

    suspend fun shouldThrottlePush(intervalMs: Long): Boolean {
        return shouldThrottle(LAST_PUSH_SENT_AT, intervalMs)
    }

    suspend fun shouldThrottleWhaleAlert(intervalMs: Long): Boolean {
        return shouldThrottle(LAST_WHALE_ALERT_SENT_AT, intervalMs)
    }

    suspend fun shouldThrottlePushTemplate(template: String, intervalMs: Long): Boolean {
        val key = when (template) {
            "whale" -> LAST_WHALE_PUSH_SENT_AT
            "daily_pulse" -> LAST_DAILY_PUSH_SENT_AT
            else -> LAST_GENERAL_PUSH_SENT_AT
        }
        return shouldThrottle(key, intervalMs)
    }

    private suspend fun shouldThrottle(key: androidx.datastore.preferences.core.Preferences.Key<Long>, intervalMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val prefs = context.dataStore.data.first()
        val lastAt = prefs[key] ?: 0L
        if (now - lastAt < intervalMs) return true
        context.dataStore.edit { updated ->
            updated[key] = now
        }
        return false
    }
}
