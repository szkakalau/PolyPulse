package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardStatsResponse(
    val alerts_24h: Int,
    val watchlist_count: Int,
    val top_movers: List<TopMover>
)

@Serializable
data class TopMover(
    val market_question: String,
    val outcome: String,
    val change: Double
)
