package com.polypulse.app.presentation.alerts

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.polypulse.app.data.remote.dto.AlertDto
import com.polypulse.app.di.AppModule
import com.polypulse.app.presentation.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AlertsState(
    val alerts: List<AlertDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String = ""
)

class AlertsViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = mutableStateOf(AlertsState())
    val state: State<AlertsState> = _state

    private val api = AppModule.backendApi
    
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
            val newAlerts = api.getAlerts()
            
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
            if (_state.value.alerts.isEmpty()) {
                _state.value = AlertsState(error = "Failed to connect to backend: ${e.message}")
            } else {
                 _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}
