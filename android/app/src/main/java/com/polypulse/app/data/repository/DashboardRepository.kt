package com.polypulse.app.data.repository

import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.BackendApi
import com.polypulse.app.data.remote.dto.DashboardStatsResponse
import com.polypulse.app.data.remote.dto.WhaleActivityDto
import kotlinx.coroutines.flow.first

class DashboardRepository(
    private val api: BackendApi,
    private val tokenManager: TokenManager
) {
    suspend fun getDashboardStats(): Result<DashboardStatsResponse> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            val stats = api.getDashboardStats("Bearer $token")
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWhaleActivity(): Result<List<WhaleActivityDto>> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            val whales = api.getWhaleActivity("Bearer $token")
            Result.success(whales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLeaderboard(): Result<List<com.polypulse.app.data.remote.dto.LeaderboardDto>> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            val leaderboard = api.getLeaderboard("Bearer $token")
            Result.success(leaderboard)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
