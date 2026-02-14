package com.polypulse.app.presentation.dashboard

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.polypulse.app.data.remote.dto.DashboardStatsResponse
import com.polypulse.app.data.remote.dto.WhaleActivityDto
import com.polypulse.app.data.repository.DashboardRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

data class DashboardState(
    val isLoading: Boolean = false,
    val stats: DashboardStatsResponse? = null,
    val whaleActivity: List<WhaleActivityDto> = emptyList(),
    val error: String? = null
)

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
                // Fetch stats and whales in parallel
                val statsDeferred = async { repository.getDashboardStats() }
                val whalesDeferred = async { repository.getWhaleActivity() }
                
                val statsResult = statsDeferred.await()
                val whalesResult = whalesDeferred.await()
                
                if (statsResult.isSuccess && whalesResult.isSuccess) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        stats = statsResult.getOrNull(),
                        whaleActivity = whalesResult.getOrNull() ?: emptyList(),
                        error = null
                    )
                } else {
                    // If either fails, show error
                    val errorMsg = statsResult.exceptionOrNull()?.message 
                        ?: whalesResult.exceptionOrNull()?.message 
                        ?: "Failed to load data"
                    
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                 _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
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
