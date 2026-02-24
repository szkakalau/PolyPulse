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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class DashboardState(
    val isLoading: Boolean = false,
    val stats: DashboardStatsResponse? = null,
    val whaleActivity: List<WhaleActivityDto> = emptyList(),
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
    private val repository: DashboardRepository
) : ViewModel() {

    private val _state = mutableStateOf(DashboardState())
    val state: State<DashboardState> = _state

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
                
                if (whalesResult.isSuccess) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        stats = statsResult.getOrNull(),
                        whaleActivity = whalesResult.getOrNull() ?: emptyList(),
                        error = null
                    )
                } else {
                    val errorMsg = whalesResult.exceptionOrNull()?.message
                        ?: statsResult.exceptionOrNull()?.message
                        ?: "Failed to load data"
                    val mappedError = if (errorMsg == "No token found") "Please login" else errorMsg
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = mappedError
                    )
                }
            } catch (e: Exception) {
                 val errorMsg = e.message ?: "Unknown error"
                 val mappedError = if (errorMsg == "No token found") "Please login" else errorMsg
                 _state.value = _state.value.copy(
                    isLoading = false,
                    error = mappedError
                )
            }
        }
    }
    
    // Alias for compatibility if needed, or just replace usage
    fun loadStats() = loadData()
}

class DashboardViewModelFactory(
    private val repository: DashboardRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class WhaleListState(
    val whales: List<WhaleActivityDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class WhaleListViewModel(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _state = mutableStateOf(WhaleListState())
    val state: State<WhaleListState> = _state

    init {
        loadWhales()
    }

    fun loadWhales() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getWhaleActivity()
            result.onSuccess { items ->
                _state.value = _state.value.copy(
                    whales = items,
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
}

class WhaleListViewModelFactory(
    private val repository: DashboardRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WhaleListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WhaleListViewModel(repository) as T
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
