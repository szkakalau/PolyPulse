package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrialStartResponseDto(
    val status: String,
    val tier: String,
    val expiresAt: String
)

