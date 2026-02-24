package com.polypulse.app.data.inapp

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "in_app_message_prefs")

class InAppMessageStore(private val context: Context) {
    companion object {
        private val LAST_MESSAGE_ID = stringPreferencesKey("last_message_id")
        private val LAST_SHOWN_AT = longPreferencesKey("last_shown_at")
    }

    suspend fun getLastShownAt(): Long? {
        val prefs = context.dataStore.data.first()
        return prefs[LAST_SHOWN_AT]
    }

    suspend fun getLastMessageId(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[LAST_MESSAGE_ID]
    }

    suspend fun updateLastShown(messageId: String, timestampMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_MESSAGE_ID] = messageId
            prefs[LAST_SHOWN_AT] = timestampMs
        }
    }
}
