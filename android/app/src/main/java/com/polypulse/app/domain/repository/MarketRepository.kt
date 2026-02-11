package com.polypulse.app.domain.repository

import com.polypulse.app.domain.model.Market

interface MarketRepository {
    suspend fun getMarkets(): Result<List<Market>>
}
