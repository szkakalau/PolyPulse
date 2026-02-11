package com.polypulse.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Market(
    val id: String,
    val question: String,
    val imageUrl: String?,
    val volume: Double,
    val outcomes: List<Outcome>,
    val tags: List<String> = emptyList()
)

@Serializable
data class Outcome(
    val name: String,
    val price: Double, // 0.0 to 1.0
    val tokenId: String
)
