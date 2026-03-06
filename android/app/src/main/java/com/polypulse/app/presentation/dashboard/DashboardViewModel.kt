package com.polypulse.app.presentation.dashboard

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.polypulse.app.data.remote.dto.DashboardStatsResponse
import com.polypulse.app.data.remote.dto.SmartWalletDto
import com.polypulse.app.data.remote.dto.WhaleActivityDto
import com.polypulse.app.data.repository.DashboardRepository
import com.polypulse.app.data.onboarding.OnboardingPreferencesStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class DashboardState(
    val isLoading: Boolean = false,
    val stats: DashboardStatsResponse? = null,
    val statsError: String? = null,
    val whaleActivity: List<WhaleActivityDto> = emptyList(),
    val filteredWhaleActivity: List<WhaleActivityDto> = emptyList(),
    val preferredCategories: Set<String> = emptySet(),
    val temporaryFilterDisabled: Boolean = false,
    val showAllPreferences: Boolean = false,
    val filterRestoreSeconds: Int = 0,
    val filterRestorePaused: Boolean = false,
    val filterRestoredJustNow: Boolean = false,
    val error: String? = null
)

private fun mapHttpError(error: Throwable, fallback: String): String {
    if (error is HttpException) {
        return when (error.code()) {
            401 -> "Please login"
            404 -> "Service unavailable"
            else -> fallback
        }
    }
    return fallback
}

class DashboardViewModel(
    private val repository: DashboardRepository,
    private val onboardingPreferencesStore: OnboardingPreferencesStore
) : ViewModel() {

    private val _state = mutableStateOf(DashboardState())
    val state: State<DashboardState> = _state
    private var filterRestoreJob: Job? = null
    private val filterRestoreDurationSeconds = 30
    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val statsDeferred = async { repository.getDashboardStats() }
                val whalesDeferred = async { repository.getWhaleActivity() }
                
                val statsResult = statsDeferred.await()
                val whalesResult = whalesDeferred.await()
                val statsError = if (statsResult.isFailure) {
                    statsResult.exceptionOrNull()?.let { mapHttpError(it, "Stats unavailable") }
                        ?: "Stats unavailable"
                } else {
                    null
                }
                
                if (whalesResult.isSuccess) {
                    val prefs = onboardingPreferencesStore.getPreferredCategories()
                    val whales = whalesResult.getOrNull() ?: emptyList()
                    val filteredWhales = applyPreferenceFilter(whales, prefs)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        stats = statsResult.getOrNull(),
                        statsError = statsError,
                        whaleActivity = whales,
                        filteredWhaleActivity = filteredWhales,
                        preferredCategories = prefs,
                        temporaryFilterDisabled = false,
                        showAllPreferences = false,
                        filterRestoreSeconds = 0,
                        filterRestorePaused = false,
                        filterRestoredJustNow = false,
                        error = null
                    )
                } else {
                    val errorMsg = whalesResult.exceptionOrNull()?.message
                        ?: statsResult.exceptionOrNull()?.message
                        ?: "Failed to load data"
                    val mappedError = if (errorMsg == "No token found") "Please login" else errorMsg
                    _state.value = _state.value.copy(
                        isLoading = false,
                        statsError = statsError,
                        error = mappedError
                    )
                }
            } catch (e: Exception) {
                 val errorMsg = e.message ?: "Unknown error"
                 val mappedError = if (errorMsg == "No token found") "Please login" else errorMsg
                 _state.value = _state.value.copy(
                    isLoading = false,
                    statsError = mappedError,
                    error = mappedError
                )
            }
        }
    }
    
    // Alias for compatibility if needed, or just replace usage
    fun loadStats() = loadData()

    fun clearPreferences() {
        viewModelScope.launch {
            onboardingPreferencesStore.setPreferredCategories(emptySet())
            onboardingPreferencesStore.setPreferenceSource("preferences")
            filterRestoreJob?.cancel()
            _state.value = _state.value.copy(
                preferredCategories = emptySet(),
                filteredWhaleActivity = _state.value.whaleActivity,
                showAllPreferences = false,
                temporaryFilterDisabled = false,
                filterRestoreSeconds = 0,
                filterRestorePaused = false,
                filterRestoredJustNow = false
            )
        }
    }

    fun disableFilterOnce() {
        filterRestoreJob?.cancel()
        _state.value = _state.value.copy(
            temporaryFilterDisabled = true,
            filterRestoreSeconds = filterRestoreDurationSeconds,
            filterRestorePaused = false,
            filterRestoredJustNow = false
        )
        startFilterRestoreCountdown(filterRestoreDurationSeconds)
    }

    fun enableFilter() {
        filterRestoreJob?.cancel()
        val wasDisabled = _state.value.temporaryFilterDisabled
        _state.value = _state.value.copy(
            temporaryFilterDisabled = false,
            filterRestoreSeconds = 0,
            filterRestorePaused = false,
            filterRestoredJustNow = wasDisabled
        )
    }

    fun pauseFilterRestore() {
        filterRestoreJob?.cancel()
        _state.value = _state.value.copy(filterRestorePaused = true)
    }

    fun resumeFilterRestore() {
        if (_state.value.filterRestoreSeconds <= 0) return
        _state.value = _state.value.copy(filterRestorePaused = false)
        startFilterRestoreCountdown(_state.value.filterRestoreSeconds)
    }

    private fun startFilterRestoreCountdown(startSeconds: Int) {
        filterRestoreJob?.cancel()
        filterRestoreJob = viewModelScope.launch {
            var remaining = startSeconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                _state.value = _state.value.copy(filterRestoreSeconds = remaining)
            }
            _state.value = _state.value.copy(
                temporaryFilterDisabled = false,
                filterRestorePaused = false,
                filterRestoredJustNow = true
            )
        }
    }
    fun togglePreferencesExpanded() {
        _state.value = _state.value.copy(showAllPreferences = !_state.value.showAllPreferences)
    }

    private fun applyPreferenceFilter(whales: List<WhaleActivityDto>, preferred: Set<String>): List<WhaleActivityDto> {
        if (preferred.isEmpty() || preferred.contains("Everything")) return whales
        return whales.filter { whale ->
            val haystack = "${whale.market_question} ${whale.market_slug}".lowercase()
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

class DashboardViewModelFactory(
    private val repository: DashboardRepository,
    private val onboardingPreferencesStore: OnboardingPreferencesStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, onboardingPreferencesStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class WhaleListState(
    val whales: List<WhaleActivityDto> = emptyList(),
    val filteredWhales: List<WhaleActivityDto> = emptyList(),
    val preferredCategories: Set<String> = emptySet(),
    val temporaryFilterDisabled: Boolean = false,
    val showAllPreferences: Boolean = false,
    val filterRestoreSeconds: Int = 0,
    val filterRestorePaused: Boolean = false,
    val filterRestoredJustNow: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class WhaleListViewModel(
    private val repository: DashboardRepository,
    private val onboardingPreferencesStore: OnboardingPreferencesStore
) : ViewModel() {

    private val _state = mutableStateOf(WhaleListState())
    val state: State<WhaleListState> = _state
    private var filterRestoreJob: Job? = null
    private val filterRestoreDurationSeconds = 30

    init {
        loadWhales()
    }

    fun loadWhales() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getWhaleActivity()
            result.onSuccess { items ->
                val prefs = onboardingPreferencesStore.getPreferredCategories()
                val filtered = applyPreferenceFilter(items, prefs)
                _state.value = _state.value.copy(
                    whales = items,
                    filteredWhales = filtered,
                    preferredCategories = prefs,
                    temporaryFilterDisabled = false,
                showAllPreferences = false,
                    filterRestoreSeconds = 0,
                    filterRestorePaused = false,
                    filterRestoredJustNow = false,
                    isLoading = false
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = mapHttpError(e, "Failed to load whales")
                )
            }
        }
    }

    fun clearPreferences() {
        viewModelScope.launch {
            onboardingPreferencesStore.setPreferredCategories(emptySet())
            onboardingPreferencesStore.setPreferenceSource("preferences")
            filterRestoreJob?.cancel()
            _state.value = _state.value.copy(
                preferredCategories = emptySet(),
                filteredWhales = _state.value.whales,
                showAllPreferences = false,
                temporaryFilterDisabled = false,
                filterRestoreSeconds = 0,
                filterRestorePaused = false,
                filterRestoredJustNow = false
            )
        }
    }

    fun disableFilterOnce() {
        filterRestoreJob?.cancel()
        _state.value = _state.value.copy(
            temporaryFilterDisabled = true,
            filterRestoreSeconds = filterRestoreDurationSeconds,
            filterRestorePaused = false,
            filterRestoredJustNow = false
        )
        startFilterRestoreCountdown(filterRestoreDurationSeconds)
    }

    fun enableFilter() {
        filterRestoreJob?.cancel()
        val wasDisabled = _state.value.temporaryFilterDisabled
        _state.value = _state.value.copy(
            temporaryFilterDisabled = false,
            filterRestoreSeconds = 0,
            filterRestorePaused = false,
            filterRestoredJustNow = wasDisabled
        )
    }

    fun pauseFilterRestore() {
        filterRestoreJob?.cancel()
        _state.value = _state.value.copy(filterRestorePaused = true)
    }

    fun resumeFilterRestore() {
        if (_state.value.filterRestoreSeconds <= 0) return
        _state.value = _state.value.copy(filterRestorePaused = false)
        startFilterRestoreCountdown(_state.value.filterRestoreSeconds)
    }

    private fun startFilterRestoreCountdown(startSeconds: Int) {
        filterRestoreJob?.cancel()
        filterRestoreJob = viewModelScope.launch {
            var remaining = startSeconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                _state.value = _state.value.copy(filterRestoreSeconds = remaining)
            }
            _state.value = _state.value.copy(
                temporaryFilterDisabled = false,
                filterRestorePaused = false,
                filterRestoredJustNow = true
            )
        }
    }

    fun togglePreferencesExpanded() {
        _state.value = _state.value.copy(showAllPreferences = !_state.value.showAllPreferences)
    }

    private fun applyPreferenceFilter(whales: List<WhaleActivityDto>, preferred: Set<String>): List<WhaleActivityDto> {
        if (preferred.isEmpty() || preferred.contains("Everything")) return whales
        return whales.filter { whale ->
            val haystack = "${whale.market_question} ${whale.market_slug}".lowercase()
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

class WhaleListViewModelFactory(
    private val repository: DashboardRepository,
    private val onboardingPreferencesStore: OnboardingPreferencesStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WhaleListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WhaleListViewModel(repository, onboardingPreferencesStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class SmartMoneyState(
    val wallets: List<SmartWalletDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SmartMoneyViewModel(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _state = mutableStateOf(SmartMoneyState())
    val state: State<SmartMoneyState> = _state

    init {
        loadSmartMoney()
    }

    fun loadSmartMoney() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getSmartWallets()
            result.onSuccess { items ->
                _state.value = _state.value.copy(
                    wallets = items,
                    isLoading = false
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = mapHttpError(e, "Failed to load smart money")
                )
            }
        }
    }
}

class SmartMoneyViewModelFactory(
    private val repository: DashboardRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmartMoneyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SmartMoneyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
