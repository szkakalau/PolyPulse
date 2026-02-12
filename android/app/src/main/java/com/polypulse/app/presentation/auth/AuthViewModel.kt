package com.polypulse.app.presentation.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.polypulse.app.data.auth.AuthRepository
import com.polypulse.app.data.remote.dto.UserResponse
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: UserResponse? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = mutableStateOf(AuthState())
    val state: State<AuthState> = _state

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            repository.getTokenFlow().collect { token ->
                if (token != null) {
                    // Validate token by fetching user
                    fetchMe()
                } else {
                    _state.value = _state.value.copy(isLoggedIn = false, user = null)
                }
            }
        }
    }

    private suspend fun fetchMe() {
        _state.value = _state.value.copy(isLoading = true)
        repository.getMe()
            .onSuccess { user ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    user = user,
                    isLoggedIn = true,
                    error = null
                )
                // Sync FCM Token
                repository.syncFcmToken()
            }
            .onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoggedIn = false,
                    user = null
                    // Don't show error for auto-login failure, just logout
                )
                repository.logout()
            }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.login(email, password)
                .onSuccess {
                    // Token saved by repo, fetchMe will be triggered by flow collector
                    // But flow might take a moment, so we can also manually trigger or wait
                    // The flow collection is separate, so it will eventually update state.
                    // For now, we can set loading to false in fetchMe
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Login failed"
                    )
                }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repository.register(email, password)
                .onSuccess {
                    // After register, auto login
                    login(email, password)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Registration failed"
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _state.value = AuthState() // Reset state
        }
    }
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
