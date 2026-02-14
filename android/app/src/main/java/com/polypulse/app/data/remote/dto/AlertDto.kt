package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlertDto(
    val timestamp: String,
    val market_question: String,
    val outcome: String,
    val old_price: Double,
    val new_price: Double,
    val change: Double,
    val message: String
)
