package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WhaleActivityDto(
    val market_question: String,
    val outcome: String,
    val side: String, // "BUY" or "SELL"
    val size: Double,
    val price: Double,
    val value_usd: Double,
    val timestamp: String, // API returns timestamp as string in some cases, or Long? Let's check Python
    val maker_address: String,
    val market_slug: String
)
