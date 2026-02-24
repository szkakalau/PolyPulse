package com.polypulse.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InAppMessageDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
    @SerialName("ctaText") val ctaText: String,
    @SerialName("ctaAction") val ctaAction: String,
    @SerialName("plans") val plans: List<PaywallPlanDto>?
)
