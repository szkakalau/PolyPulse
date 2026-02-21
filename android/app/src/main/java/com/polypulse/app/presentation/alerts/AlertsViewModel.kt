package com.polypulse.app.presentation.alerts

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.dto.AlertDto
import com.polypulse.app.di.AppModule
import com.polypulse.app.presentation.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

data class AlertsState(
    val alerts: List<AlertDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String = ""
)

class AlertsViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = mutableStateOf(AlertsState())
    val state: State<AlertsState> = _state

    private val apiProvider = AppModule.backendApiProvider
    private val tokenManager = TokenManager(application)
    
    // Store the timestamp of the latest alert we've seen
    private var lastAlertTimestamp: String = ""

    init {
        startPolling()
    }

    fun refreshAlerts() {
        // Just trigger a fetch immediately (polling will continue)
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            fetchAlerts()
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            while (isActive) {
                fetchAlerts()
                delay(10000) // Poll every 10 seconds
            }
        }
    }

    private suspend fun fetchAlerts() {
        try {
            val token = tokenManager.token.first()
            if (token.isNullOrBlank()) {
                _state.value = AlertsState(error = "Please login to view alerts")
                return
            }

            val newAlerts = apiProvider.call { it.getAlerts("Bearer $token") }
            
            if (newAlerts.isNotEmpty()) {
                // Check for new alerts to notify
                val latestAlert = newAlerts.last() 
                
                // Simple check: if we have seen alerts before, and this timestamp is newer
                if (lastAlertTimestamp.isNotEmpty() && latestAlert.timestamp > lastAlertTimestamp) {
                     NotificationHelper.showNotification(
                         getApplication(),
                         "PolyPulse Alert",
                         latestAlert.message
                     )
                }
                
                // Update our "last seen" to the very newest one
                lastAlertTimestamp = latestAlert.timestamp
            }

            _state.value = AlertsState(alerts = newAlerts.reversed(), isLoading = false) 
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 401) {
                tokenManager.deleteToken()
            }
            if (_state.value.alerts.isEmpty()) {
                _state.value = AlertsState(error = mapErrorMessage(e))
            } else {
                 _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private fun mapErrorMessage(error: Exception): String {
        if (error is HttpException) {
            return when (error.code()) {
                401 -> "Please login to view alerts"
                404 -> "Alerts service unavailable"
                else -> "Failed to load alerts"
            }
        }
        return "Failed to connect to backend"
    }
}
