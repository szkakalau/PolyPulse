package com.polypulse.app.data.remote

import com.polypulse.app.data.remote.dto.AlertDto
import com.polypulse.app.data.remote.dto.AuthResponse
import com.polypulse.app.data.remote.dto.LoginRequest
import com.polypulse.app.data.remote.dto.PaywallResponseDto
import com.polypulse.app.data.remote.dto.RegisterRequest
import com.polypulse.app.data.remote.dto.SignalDto
import com.polypulse.app.data.remote.dto.SmartWalletDto
import com.polypulse.app.data.remote.dto.TrialStartResponseDto
import com.polypulse.app.data.remote.dto.EntitlementsResponseDto
import com.polypulse.app.data.remote.dto.UserResponse
import com.polypulse.app.data.remote.dto.BillingVerifyRequestDto
import com.polypulse.app.data.remote.dto.BillingVerifyResponseDto
import com.polypulse.app.data.remote.dto.BillingStatusResponseDto
import com.polypulse.app.data.remote.dto.NotificationSettingsDto
import com.polypulse.app.data.remote.dto.NotificationSettingsUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import com.polypulse.app.data.remote.dto.AnalyticsEventRequest
import com.polypulse.app.data.remote.dto.AnalyticsEventResponse

interface BackendApi {
    @GET("dashboard/alerts")
    suspend fun getAlerts(@Header("Authorization") token: String): List<com.polypulse.app.data.remote.dto.AlertDto>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") pass: String
    ): AuthResponse

    @GET("users/me")
    suspend fun getMe(@Header("Authorization") token: String): UserResponse

    @GET("watchlist")
    suspend fun getWatchlist(@Header("Authorization") token: String): List<String>

    @POST("watchlist/{marketId}")
    suspend fun addToWatchlist(
        @Header("Authorization") token: String,
        @Path("marketId") marketId: String
    ): Unit

    @DELETE("watchlist/{marketId}")
    suspend fun removeFromWatchlist(
        @Header("Authorization") token: String,
        @Path("marketId") marketId: String
    ): Unit

    @POST("notifications/register")
    suspend fun registerFCMToken(
        @Header("Authorization") token: String,
        @Body request: com.polypulse.app.data.remote.dto.FCMTokenRequest
    ): Unit

    @GET("notification-settings")
    suspend fun getNotificationSettings(
        @Header("Authorization") token: String
    ): NotificationSettingsDto

    @PUT("notification-settings")
    suspend fun updateNotificationSettings(
        @Header("Authorization") token: String,
        @Body request: NotificationSettingsUpdateRequestDto
    ): NotificationSettingsDto

    @GET("dashboard/stats")
    suspend fun getDashboardStats(
        @Header("Authorization") token: String
    ): com.polypulse.app.data.remote.dto.DashboardStatsResponse

    @GET("dashboard/whales")
    suspend fun getWhaleActivity(
        @Header("Authorization") token: String
    ): List<com.polypulse.app.data.remote.dto.WhaleActivityDto>

    @GET("dashboard/leaderboard")
    suspend fun getLeaderboard(
        @Header("Authorization") token: String
    ): List<com.polypulse.app.data.remote.dto.LeaderboardDto>

    @GET("api/whales")
    suspend fun getPublicWhales(): List<com.polypulse.app.data.remote.dto.WhaleActivityDto>

    @GET("api/smart")
    suspend fun getSmartWallets(): List<SmartWalletDto>

    @GET("signals")
    suspend fun getSignals(
        @Header("Authorization") token: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<SignalDto>

    @GET("signals/{signalId}")
    suspend fun getSignalDetail(
        @Header("Authorization") token: String? = null,
        @Path("signalId") signalId: Int
    ): SignalDto

    @GET("paywall")
    suspend fun getPaywall(): PaywallResponseDto

    @POST("trial/start")
    suspend fun startTrial(
        @Header("Authorization") token: String
    ): TrialStartResponseDto

    @GET("entitlements/me")
    suspend fun getEntitlementsMe(
        @Header("Authorization") token: String
    ): EntitlementsResponseDto

    @POST("billing/verify")
    suspend fun verifyBilling(
        @Header("Authorization") token: String,
        @Body request: BillingVerifyRequestDto
    ): BillingVerifyResponseDto

    @GET("billing/status")
    suspend fun getBillingStatus(
        @Header("Authorization") token: String
    ): BillingStatusResponseDto

    @POST("analytics/event")
    suspend fun trackEvent(
        @Header("Authorization") token: String? = null,
        @Body request: AnalyticsEventRequest
    ): AnalyticsEventResponse
}

class BackendApiProvider(private val apis: List<BackendApi>) {
    suspend fun <T> call(block: suspend (BackendApi) -> T): T {
        var last: Exception? = null
        for (api in apis) {
            try {
                return block(api)
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: Exception("Backend unavailable")
    }
}
