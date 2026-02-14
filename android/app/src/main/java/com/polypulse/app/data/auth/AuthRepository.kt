package com.polypulse.app.data.auth

import com.polypulse.app.data.remote.BackendApi
import com.polypulse.app.data.remote.dto.LoginRequest
import com.polypulse.app.data.remote.dto.RegisterRequest
import com.polypulse.app.data.remote.dto.UserResponse
import kotlinx.coroutines.flow.first

import com.polypulse.app.data.remote.dto.FCMTokenRequest

class AuthRepository(
    private val api: BackendApi,
    private val tokenManager: TokenManager
) {
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = api.login(email, password)
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
            val response = api.register(RegisterRequest(email, password))
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
            val response = api.getMe("Bearer $token")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncFcmToken(): Result<Unit> {
        return try {
            val jwtToken = tokenManager.token.first() ?: return Result.failure(Exception("No JWT token"))
            val fcmToken = tokenManager.fcmToken.first() ?: return Result.failure(Exception("No FCM token"))
            
            api.registerFCMToken("Bearer $jwtToken", FCMTokenRequest(fcmToken))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getTokenFlow() = tokenManager.token
}
