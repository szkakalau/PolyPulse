package com.polypulse.app.data.onboarding

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "onboarding_prefs")

class OnboardingPreferencesStore(private val context: Context) {
    companion object {
        private val CATEGORY_PREFERENCES = stringSetPreferencesKey("category_preferences")
        private val PREF_FILTER_ENABLED = booleanPreferencesKey("pref_filter_enabled")
        private val PREF_SOURCE = stringPreferencesKey("pref_source")
    }

    val preferredCategories: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[CATEGORY_PREFERENCES] ?: emptySet()
    }

    suspend fun setPreferredCategories(categories: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[CATEGORY_PREFERENCES] = categories
        }
    }

    suspend fun getPreferredCategories(): Set<String> {
        return preferredCategories.first()
    }

    suspend fun setPreferenceFilterEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PREF_FILTER_ENABLED] = enabled
        }
    }

    suspend fun getPreferenceFilterEnabled(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[PREF_FILTER_ENABLED] ?: true
    }

    suspend fun setPreferenceSource(source: String) {
        context.dataStore.edit { prefs ->
            prefs[PREF_SOURCE] = source
        }
    }

    suspend fun getPreferenceSource(): String {
        val prefs = context.dataStore.data.first()
        return prefs[PREF_SOURCE] ?: "onboarding"
    }
}
