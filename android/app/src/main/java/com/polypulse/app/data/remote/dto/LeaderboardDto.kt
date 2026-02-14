package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardDto(
    val maker_address: String,
    val total_volume: Double,
    val trade_count: Int,
    val max_trade_value: Double,
    val buy_volume: Double,
    val sell_volume: Double
)
