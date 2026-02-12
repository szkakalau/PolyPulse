package com.polypulse.app.data.remote

import com.polypulse.app.data.remote.dto.AlertResponse
import com.polypulse.app.data.remote.dto.AuthResponse
import com.polypulse.app.data.remote.dto.LoginRequest
import com.polypulse.app.data.remote.dto.RegisterRequest
import com.polypulse.app.data.remote.dto.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface BackendApi {
    @GET("alerts")
    suspend fun getAlerts(): AlertResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") token: String): UserResponse
}
