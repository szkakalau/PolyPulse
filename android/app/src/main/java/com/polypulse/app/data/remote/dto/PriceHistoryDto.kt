package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PriceHistoryResponse(
    val history: List<PricePointDto>
)

@Serializable
data class PricePointDto(
    val t: Long, // timestamp
    val p: Double // price
)
