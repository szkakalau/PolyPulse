package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaywallPlanDto(
    val id: String,
    val name: String,
    val price: Int,
    val currency: String,
    val period: String,
    val trialDays: Int
)

@Serializable
data class PaywallResponseDto(
    val plans: List<PaywallPlanDto>
)

@Serializable
data class SignalStatsDto(
    val signals7d: Int,
    val evidence7d: Int
)
