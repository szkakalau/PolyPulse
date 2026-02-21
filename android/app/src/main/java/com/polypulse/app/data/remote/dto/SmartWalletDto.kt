package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SmartWalletDto(
    val address: String,
    val profit: Double,
    val roi: Double,
    val win_rate: Double,
    val total_trades: Int
)
