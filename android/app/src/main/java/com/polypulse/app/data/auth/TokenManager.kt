package com.polypulse.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
        @Volatile
        private var debugTokenOverride: String? = null

        fun setDebugTokenOverride(token: String?) {
            debugTokenOverride = token?.takeIf { it.isNotBlank() }
        }
    }

    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        debugTokenOverride ?: preferences[TOKEN_KEY] ?: readDebugToken()
    }

    val fcmToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FCM_TOKEN_KEY]
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
        setDebugTokenOverride(token)
        runCatching {
            File(context.filesDir, "debug_token.txt").writeText(token)
        }
    }

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[FCM_TOKEN_KEY] = token
        }
    }

    suspend fun deleteToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
        setDebugTokenOverride(null)
        runCatching {
            File(context.filesDir, "debug_token.txt").delete()
        }
    }

    private fun readDebugToken(): String? {
        val file = File(context.filesDir, "debug_token.txt")
        if (!file.exists()) return null
        val token = file.readText().trim()
        return if (token.isBlank()) null else token
    }
}
