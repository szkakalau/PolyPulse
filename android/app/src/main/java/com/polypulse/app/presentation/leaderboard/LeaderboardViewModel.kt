package com.polypulse.app.presentation.leaderboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.polypulse.app.data.remote.dto.SmartWalletDto
import com.polypulse.app.data.repository.DashboardRepository
import kotlinx.coroutines.launch

data class LeaderboardState(
    val leaderboard: List<SmartWalletDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class LeaderboardViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    var state by mutableStateOf(LeaderboardState())
        private set

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            val result = dashboardRepository.getSmartWallets()
            
            result.onSuccess { items ->
                state = state.copy(
                    leaderboard = items,
                    isLoading = false
                )
            }.onFailure { e ->
                state = state.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load leaderboard"
                )
            }
        }
    }
}

class LeaderboardViewModelFactory(
    private val repository: DashboardRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LeaderboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
