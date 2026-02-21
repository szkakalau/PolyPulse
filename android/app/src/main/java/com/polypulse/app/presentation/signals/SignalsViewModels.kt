package com.polypulse.app.presentation.signals

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.polypulse.app.data.remote.dto.SignalDto
import com.polypulse.app.data.repository.SignalsRepository
import kotlinx.coroutines.launch

data class SignalsListState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val signals: List<SignalDto> = emptyList()
)

class SignalsListViewModel(private val repository: SignalsRepository) : ViewModel() {
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
                    _state.value = _state.value.copy(isLoading = false, signals = rows, error = null)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load signals")
                }
        }
    }
}

class SignalsListViewModelFactory(private val repository: SignalsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignalsListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignalsListViewModel(repository) as T
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

