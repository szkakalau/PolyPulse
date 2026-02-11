package com.polypulse.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MarketResponse(
    val data: List<MarketDto>,
    @SerialName("next_cursor") val nextCursor: String? = null
)

@Serializable
data class MarketDto(
    @SerialName("condition_id") val conditionId: String,
    val question: String,
    val tokens: List<TokenDto>? = null,
    val volume: Double? = null, // Sometimes volume is string or number, assuming double or null
    val liquidity: Double? = null,
    val description: String? = null,
    val end_date_iso: String? = null,
    val image: String? = null,
    val tags: List<String>? = null
)

@Serializable
data class TokenDto(
    @SerialName("token_id") val tokenId: String,
    @SerialName("outcome") val outcome: String, // "Yes" or "No"
    val price: Double? = null // Estimated price if available in market object
)

@Serializable
data class OrderBookDto(
    val bids: List<OrderDto>,
    val asks: List<OrderDto>
)

@Serializable
data class OrderDto(
    val price: String,
    val size: String
)
