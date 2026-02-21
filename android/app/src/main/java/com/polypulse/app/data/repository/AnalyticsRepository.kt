package com.polypulse.app.data.repository

import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.BackendApiProvider
import com.polypulse.app.data.remote.dto.AnalyticsEventRequest
import com.polypulse.app.data.remote.dto.AnalyticsEventResponse
import kotlinx.coroutines.flow.first

class AnalyticsRepository(
    private val apiProvider: BackendApiProvider,
    private val tokenManager: TokenManager
) {
    suspend fun trackEvent(
        eventName: String,
        properties: Map<String, String>? = null
    ): Result<AnalyticsEventResponse> {
        return try {
            val token = tokenManager.token.first()
            val authHeader = token?.let { "Bearer $it" }
            val request = AnalyticsEventRequest(eventName = eventName, properties = properties)
            val response = apiProvider.call { it.trackEvent(authHeader, request) }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
