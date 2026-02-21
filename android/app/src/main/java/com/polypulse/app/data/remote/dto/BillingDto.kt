package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BillingVerifyRequestDto(
    val purchaseToken: String,
    val productId: String,
    val platform: String
)

@Serializable
data class SubscriptionInfoDto(
    val status: String,
    val planId: String,
    val startAt: String,
    val endAt: String,
    val autoRenew: Boolean
)

@Serializable
data class BillingVerifyResponseDto(
    val status: String,
    val subscription: SubscriptionInfoDto,
    val entitlements: EntitlementsResponseDto
)

@Serializable
data class BillingStatusResponseDto(
    val status: String,
    val planId: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val autoRenew: Boolean? = null
)

