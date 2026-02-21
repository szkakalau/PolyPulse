package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsEventRequest(
    val eventName: String,
    val properties: Map<String, String>? = null
)

@Serializable
data class AnalyticsEventResponse(
    val status: String
)
