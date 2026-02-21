package com.polypulse.app.data.repository

import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.BackendApiProvider
import com.polypulse.app.data.remote.dto.NotificationSettingsDto
import com.polypulse.app.data.remote.dto.NotificationSettingsUpdateRequestDto
import kotlinx.coroutines.flow.first

class NotificationSettingsRepository(
    private val apiProvider: BackendApiProvider,
    private val tokenManager: TokenManager
) {
    suspend fun getSettings(): Result<NotificationSettingsDto> {
        return try {
            val token = tokenManager.token.first()
            val authHeader = token?.let { "Bearer $it" } ?: return Result.failure(Exception("Not logged in"))
            val response = apiProvider.call { it.getNotificationSettings(authHeader) }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSettings(enabled: Boolean): Result<NotificationSettingsDto> {
        return try {
            val token = tokenManager.token.first()
            val authHeader = token?.let { "Bearer $it" } ?: return Result.failure(Exception("Not logged in"))
            val response = apiProvider.call {
                it.updateNotificationSettings(authHeader, NotificationSettingsUpdateRequestDto(enabled = enabled))
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
