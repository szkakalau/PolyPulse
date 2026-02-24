package com.polypulse.app.data.repository

import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.BackendApiProvider
import com.polypulse.app.data.remote.dto.BillingStatusResponseDto
import com.polypulse.app.data.remote.dto.BillingVerifyRequestDto
import com.polypulse.app.data.remote.dto.BillingVerifyResponseDto
import com.polypulse.app.data.remote.dto.EntitlementsResponseDto
import com.polypulse.app.data.remote.dto.PaywallResponseDto
import com.polypulse.app.data.remote.dto.SignalStatsDto
import com.polypulse.app.data.remote.dto.TrialStartResponseDto
import kotlinx.coroutines.flow.first

class PaywallRepository(
    private val apiProvider: BackendApiProvider,
    private val tokenManager: TokenManager
) {
    suspend fun getPaywall(): Result<PaywallResponseDto> {
        return try {
            val res = apiProvider.call { it.getPaywall() }
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startTrial(): Result<TrialStartResponseDto> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            val res = apiProvider.call { it.startTrial("Bearer $token") }
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEntitlementsMe(): Result<EntitlementsResponseDto> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            val res = apiProvider.call { it.getEntitlementsMe("Bearer $token") }
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyBilling(productId: String): Result<BillingVerifyResponseDto> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            val request = BillingVerifyRequestDto(
                purchaseToken = "stub-${System.currentTimeMillis()}",
                productId = productId,
                platform = "android_stub"
            )
            val res = apiProvider.call { it.verifyBilling("Bearer $token", request) }
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBillingStatus(): Result<BillingStatusResponseDto> {
        return try {
            val token = tokenManager.token.first() ?: throw Exception("No token found")
            val res = apiProvider.call { it.getBillingStatus("Bearer $token") }
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSignalStats(): Result<SignalStatsDto> {
        return try {
            val res = apiProvider.call { it.getSignalStats() }
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
