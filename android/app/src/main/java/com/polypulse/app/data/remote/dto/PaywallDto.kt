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

@Serializable
data class SignalCredibilityHistogramItem(
    val bucket: String,
    val count: Int
)

@Serializable
data class SignalCredibilityWindowResponse(
    val windowDays: Int,
    val signalsTotal: Int,
    val signalsWithEvidence: Int,
    val evidenceRate: Double,
    val evaluatedTotal: Int,
    val hitTotal: Int,
    val hitRate: Double,
    val hitRateCiLow: Double,
    val hitRateCiHigh: Double,
    val latencyCount: Int,
    val latencyP50Seconds: Int? = null,
    val latencyP90Seconds: Int? = null,
    val latencyHistogram: List<SignalCredibilityHistogramItem>,
    val leadCount: Int,
    val leadP50Seconds: Int? = null,
    val leadP90Seconds: Int? = null,
    val leadHistogram: List<SignalCredibilityHistogramItem>
)

@Serializable
data class SignalCredibilityResponse(
    val window7d: SignalCredibilityWindowResponse,
    val window30d: SignalCredibilityWindowResponse
)
