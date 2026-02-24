package com.polypulse.app.presentation.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.polypulse.app.data.remote.dto.BillingStatusResponseDto
import com.polypulse.app.data.remote.dto.EntitlementsResponseDto
import com.polypulse.app.data.remote.dto.PaywallPlanDto
import com.polypulse.app.data.remote.dto.SignalStatsDto
import com.polypulse.app.data.repository.PaywallRepository
import kotlinx.coroutines.launch

data class PaywallState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val plans: List<PaywallPlanDto> = emptyList(),
    val trialResult: String? = null,
    val entitlements: EntitlementsResponseDto? = null,
    val billingStatus: BillingStatusResponseDto? = null,
    val signalStats: SignalStatsDto? = null
)

class PaywallViewModel(private val repository: PaywallRepository) : ViewModel() {
    private val _state = mutableStateOf(PaywallState())
    val state: State<PaywallState> = _state

    init {
        loadPaywall()
        loadSignalStats()
    }

    fun loadPaywall() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.getPaywall()
                .onSuccess { res ->
                    _state.value = _state.value.copy(isLoading = false, plans = res.plans, error = null)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load paywall")
                }
        }
    }

    fun loadSignalStats() {
        viewModelScope.launch {
            repository.getSignalStats()
                .onSuccess { stats ->
                    _state.value = _state.value.copy(signalStats = stats)
                }
        }
    }

    fun refreshEntitlements() {
        viewModelScope.launch {
            repository.getEntitlementsMe()
                .onSuccess { ent ->
                    _state.value = _state.value.copy(entitlements = ent)
                }
        }
    }

    fun refreshBillingStatus() {
        viewModelScope.launch {
            repository.getBillingStatus()
                .onSuccess { status ->
                    _state.value = _state.value.copy(billingStatus = status)
                }
        }
    }

    fun startTrial(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, trialResult = null)
            repository.startTrial()
                .onSuccess { res ->
                    _state.value = _state.value.copy(isLoading = false, trialResult = res.status)
                    refreshEntitlements()
                    refreshBillingStatus()
                    onSuccess()
                }
                .onFailure { e ->
                    val errorMessage = e.message ?: "Failed to start trial"
                    _state.value = _state.value.copy(isLoading = false, error = errorMessage)
                    onFailure(errorMessage)
                }
        }
    }

    fun subscribeStub(productId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.verifyBilling(productId)
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                    refreshEntitlements()
                    refreshBillingStatus()
                    onSuccess()
                }
                .onFailure { e ->
                    val errorMessage = e.message ?: "Failed to subscribe"
                    _state.value = _state.value.copy(isLoading = false, error = errorMessage)
                    onFailure(errorMessage)
                }
        }
    }
}

class PaywallViewModelFactory(private val repository: PaywallRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaywallViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaywallViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
