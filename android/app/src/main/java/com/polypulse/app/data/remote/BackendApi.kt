package com.polypulse.app.data.remote

import com.polypulse.app.data.remote.dto.AlertResponse
import com.polypulse.app.data.remote.dto.AuthResponse
import com.polypulse.app.data.remote.dto.LoginRequest
import com.polypulse.app.data.remote.dto.RegisterRequest
import com.polypulse.app.data.remote.dto.UserResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface BackendApi {
    @GET("alerts")
    suspend fun getAlerts(): AlertResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("auth/me")
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
}
