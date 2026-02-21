package com.polypulse.app.data.auth

import com.polypulse.app.data.remote.BackendApiProvider
import com.polypulse.app.data.remote.dto.LoginRequest
import com.polypulse.app.data.remote.dto.RegisterRequest
import com.polypulse.app.data.remote.dto.UserResponse
import kotlinx.coroutines.flow.first

import com.polypulse.app.data.remote.dto.FCMTokenRequest

class AuthRepository(
    private val apiProvider: BackendApiProvider,
    private val tokenManager: TokenManager
) {
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = apiProvider.call { it.login(email, password) }
            tokenManager.saveToken(response.access_token)
            // Attempt to sync FCM token after login
            syncFcmToken()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String): Result<UserResponse> {
        return try {
            val response = apiProvider.call { it.register(RegisterRequest(email, password)) }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.deleteToken()
    }

    suspend fun getMe(): Result<UserResponse> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            val response = apiProvider.call { it.getMe("Bearer $token") }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncFcmToken(): Result<Unit> {
        return try {
            val jwtToken = tokenManager.token.first() ?: return Result.failure(Exception("No JWT token"))
            val fcmToken = tokenManager.fcmToken.first() ?: return Result.failure(Exception("No FCM token"))
            
            apiProvider.call { it.registerFCMToken("Bearer $jwtToken", FCMTokenRequest(fcmToken)) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isUserLoggedIn(): Boolean {
        val token = tokenManager.token.first()
        return !token.isNullOrBlank()
    }
    
    fun getTokenFlow() = tokenManager.token
}
