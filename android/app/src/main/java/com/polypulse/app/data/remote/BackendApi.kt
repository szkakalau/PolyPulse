package com.polypulse.app.data.remote

import com.polypulse.app.data.remote.dto.AlertResponse
import com.polypulse.app.data.remote.dto.AuthResponse
import com.polypulse.app.data.remote.dto.LoginRequest
import com.polypulse.app.data.remote.dto.RegisterRequest
import com.polypulse.app.data.remote.dto.UserResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface BackendApi {
    @GET("dashboard/alerts")
    suspend fun getAlerts(): List<com.polypulse.app.data.remote.dto.AlertDto>

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
}
