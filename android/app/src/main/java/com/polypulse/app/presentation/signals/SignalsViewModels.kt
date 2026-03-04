package com.polypulse.app.presentation.signals

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.polypulse.app.data.remote.dto.SignalDto
import com.polypulse.app.data.repository.SignalsRepository
import com.polypulse.app.data.onboarding.OnboardingPreferencesStore
import kotlinx.coroutines.launch

data class SignalsListState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val signals: List<SignalDto> = emptyList(),
    val filteredSignals: List<SignalDto> = emptyList(),
    val preferredCategories: Set<String> = emptySet(),
    val preferenceFilterEnabled: Boolean = true,
    val preferenceSource: String = "onboarding",
    val temporaryFilterDisabled: Boolean = false
)

class SignalsListViewModel(
    private val repository: SignalsRepository,
    private val onboardingPreferencesStore: OnboardingPreferencesStore
) : ViewModel() {
    private val _state = mutableStateOf(SignalsListState())
    val state: State<SignalsListState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.getSignals()
                .onSuccess { rows ->
                    val prefs = onboardingPreferencesStore.getPreferredCategories()
                    val filterEnabled = onboardingPreferencesStore.getPreferenceFilterEnabled()
                    val source = onboardingPreferencesStore.getPreferenceSource()
                    val filtered = applyPreferenceFilter(rows, prefs, filterEnabled && !_state.value.temporaryFilterDisabled)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        signals = rows,
                        filteredSignals = filtered,
                        preferredCategories = prefs,
                        preferenceFilterEnabled = filterEnabled,
                        preferenceSource = source,
                        temporaryFilterDisabled = false,
                        error = null
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load signals")
                }
        }
    }

    fun clearPreferenceFilter() {
        viewModelScope.launch {
            onboardingPreferencesStore.setPreferredCategories(emptySet())
            onboardingPreferencesStore.setPreferenceSource("preferences")
            val filtered = applyPreferenceFilter(
                _state.value.signals,
                emptySet(),
                _state.value.preferenceFilterEnabled && !_state.value.temporaryFilterDisabled
            )
            _state.value = _state.value.copy(
                filteredSignals = filtered,
                preferredCategories = emptySet(),
                preferenceSource = "preferences",
                temporaryFilterDisabled = false
            )
        }
    }

    fun clearAndDisablePreferences() {
        viewModelScope.launch {
            onboardingPreferencesStore.setPreferredCategories(emptySet())
            onboardingPreferencesStore.setPreferenceFilterEnabled(false)
            onboardingPreferencesStore.setPreferenceSource("preferences")
            _state.value = _state.value.copy(
                preferredCategories = emptySet(),
                preferenceFilterEnabled = false,
                filteredSignals = _state.value.signals,
                preferenceSource = "preferences",
                temporaryFilterDisabled = false
            )
        }
    }

    fun setPreferenceFilterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            onboardingPreferencesStore.setPreferenceFilterEnabled(enabled)
            val filtered = applyPreferenceFilter(
                _state.value.signals,
                _state.value.preferredCategories,
                enabled && !_state.value.temporaryFilterDisabled
            )
            _state.value = _state.value.copy(
                preferenceFilterEnabled = enabled,
                temporaryFilterDisabled = false,
                filteredSignals = filtered
            )
        }
    }

    fun resetPreferences() {
        viewModelScope.launch {
            onboardingPreferencesStore.setPreferredCategories(emptySet())
            onboardingPreferencesStore.setPreferenceFilterEnabled(true)
            onboardingPreferencesStore.setPreferenceSource("preferences")
            _state.value = _state.value.copy(
                preferredCategories = emptySet(),
                preferenceFilterEnabled = true,
                filteredSignals = _state.value.signals,
                preferenceSource = "preferences",
                temporaryFilterDisabled = false
            )
        }
    }

    fun saveCurrentPreferencesAsDefault() {
        viewModelScope.launch {
            onboardingPreferencesStore.setPreferredCategories(_state.value.preferredCategories)
            onboardingPreferencesStore.setPreferenceSource("preferences")
            _state.value = _state.value.copy(
                preferenceSource = "preferences"
            )
        }
    }

    fun setEverythingPreference() {
        viewModelScope.launch {
            onboardingPreferencesStore.setPreferredCategories(setOf("Everything"))
            onboardingPreferencesStore.setPreferenceFilterEnabled(true)
            onboardingPreferencesStore.setPreferenceSource("preferences")
            val filtered = applyPreferenceFilter(_state.value.signals, setOf("Everything"), true)
            _state.value = _state.value.copy(
                preferredCategories = setOf("Everything"),
                preferenceFilterEnabled = true,
                filteredSignals = filtered,
                preferenceSource = "preferences",
                temporaryFilterDisabled = false
            )
        }
    }

    fun disableFilterOnce() {
        _state.value = _state.value.copy(
            temporaryFilterDisabled = true,
            filteredSignals = _state.value.signals
        )
    }

    fun enableFilter() {
        val filtered = applyPreferenceFilter(
            _state.value.signals,
            _state.value.preferredCategories,
            _state.value.preferenceFilterEnabled
        )
        _state.value = _state.value.copy(
            temporaryFilterDisabled = false,
            filteredSignals = filtered
        )
    }

    private fun applyPreferenceFilter(signals: List<SignalDto>, preferred: Set<String>, enabled: Boolean): List<SignalDto> {
        if (!enabled) return signals
        if (preferred.isEmpty() || preferred.contains("Everything")) return signals
        return signals.filter { signal ->
            val haystack = buildString {
                append(signal.title)
                signal.content?.let { append(" ").append(it) }
                signal.evidence?.let { append(" ").append(it.marketId).append(" ").append(it.sourceType) }
            }.lowercase()
            preferred.any { pref ->
                when (pref) {
                    "Politics" -> haystack.contains("trump") || haystack.contains("biden") || haystack.contains("election") || haystack.contains("politic")
                    "Crypto" -> haystack.contains("bitcoin") || haystack.contains("ethereum") || haystack.contains("crypto") || haystack.contains("token")
                    "Sports" -> haystack.contains("nba") || haystack.contains("nfl") || haystack.contains("soccer") || haystack.contains("sport")
                    "Macro" -> haystack.contains("inflation") || haystack.contains("rates") || haystack.contains("fed") || haystack.contains("economy")
                    else -> true
                }
            }
        }
    }
}

class SignalsListViewModelFactory(private val repository: SignalsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignalsListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            throw IllegalArgumentException("Use factory that provides onboarding preferences store")
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SignalsListViewModelWithPrefsFactory(
    private val repository: SignalsRepository,
    private val onboardingPreferencesStore: OnboardingPreferencesStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignalsListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignalsListViewModel(repository, onboardingPreferencesStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class SignalDetailState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val signal: SignalDto? = null
)

class SignalDetailViewModel(private val repository: SignalsRepository) : ViewModel() {
    private val _state = mutableStateOf(SignalDetailState())
    val state: State<SignalDetailState> = _state

    fun load(signalId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.getSignalDetail(signalId)
                .onSuccess { row ->
                    _state.value = _state.value.copy(isLoading = false, signal = row, error = null)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load signal")
                }
        }
    }
}

class SignalDetailViewModelFactory(private val repository: SignalsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignalDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignalDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
