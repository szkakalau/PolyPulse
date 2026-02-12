package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FCMTokenRequest(
    val token: String
)
