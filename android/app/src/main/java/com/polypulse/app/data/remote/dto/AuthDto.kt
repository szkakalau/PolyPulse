package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val access_token: String,
    val token_type: String
)

@Serializable
data class UserResponse(
    val id: Int,
    val email: String,
    val created_at: String
)
