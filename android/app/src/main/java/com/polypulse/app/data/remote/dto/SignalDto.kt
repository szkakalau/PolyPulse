package com.polypulse.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SignalEvidenceDto(
    val sourceType: String,
    val triggeredAt: String,
    val marketId: String,
    val makerAddress: String,
    val evidenceUrl: String,
    val dedupeKey: String
)

@Serializable
data class SignalDto(
    val id: Int,
    val title: String,
    val content: String? = null,
    val locked: Boolean,
    val tierRequired: String,
    val createdAt: String,
    val evidence: SignalEvidenceDto? = null
)

