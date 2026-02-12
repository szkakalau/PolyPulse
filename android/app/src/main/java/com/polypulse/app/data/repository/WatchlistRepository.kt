package com.polypulse.app.data.repository

import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.BackendApi
import kotlinx.coroutines.flow.first

class WatchlistRepository(
    private val api: BackendApi,
    private val tokenManager: TokenManager
) {
    suspend fun getWatchlist(): Result<List<String>> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            val watchlist = api.getWatchlist("Bearer $token")
            Result.success(watchlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToWatchlist(marketId: String): Result<Unit> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            api.addToWatchlist("Bearer $token", marketId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromWatchlist(marketId: String): Result<Unit> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            api.removeFromWatchlist("Bearer $token", marketId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
