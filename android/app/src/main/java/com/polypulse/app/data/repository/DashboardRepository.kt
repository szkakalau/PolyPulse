package com.polypulse.app.data.repository

import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.BackendApiProvider
import com.polypulse.app.data.remote.dto.DashboardStatsResponse
import com.polypulse.app.data.remote.dto.SmartWalletDto
import com.polypulse.app.data.remote.dto.WhaleActivityDto
import kotlinx.coroutines.flow.first

class DashboardRepository(
    private val apiProvider: BackendApiProvider,
    private val tokenManager: TokenManager
) {
    suspend fun getDashboardStats(): Result<DashboardStatsResponse> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            val stats = apiProvider.call { it.getDashboardStats("Bearer $token") }
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWhaleActivity(): Result<List<WhaleActivityDto>> {
        return try {
            val whales = apiProvider.call { it.getPublicWhales() }
            Result.success(whales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSmartWallets(): Result<List<SmartWalletDto>> {
        return try {
            val wallets = apiProvider.call { it.getSmartWallets() }
            Result.success(wallets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
