package com.polypulse.app.data.repository

import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.BackendApiProvider
import com.polypulse.app.data.remote.dto.SignalDto
import kotlinx.coroutines.flow.first

class SignalsRepository(
    private val apiProvider: BackendApiProvider,
    private val tokenManager: TokenManager
) {
    suspend fun getSignals(limit: Int = 50, offset: Int = 0): Result<List<SignalDto>> {
        return try {
            val token = tokenManager.token.first()
            val authHeader = token?.let { "Bearer $it" }
            val rows = apiProvider.call { it.getSignals(authHeader, limit, offset) }
            Result.success(rows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSignalDetail(signalId: Int): Result<SignalDto> {
        return try {
            val token = tokenManager.token.first()
            val authHeader = token?.let { "Bearer $it" }
            val row = apiProvider.call { it.getSignalDetail(authHeader, signalId) }
            Result.success(row)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

