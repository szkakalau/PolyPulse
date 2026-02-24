package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class EntitlementFeatureDto(
    val key: String,
    val enabled: Boolean,
    val quota: Int? = null
)

@Serializable
data class EntitlementsResponseDto(
    val tier: String,
    val features: List<EntitlementFeatureDto>,
    val effectiveAt: String? = null,
    val expiresAt: String? = null
)
