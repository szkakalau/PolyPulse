package com.polypulse.app.data.repository

import com.polypulse.app.data.remote.PolymarketApi
import com.polypulse.app.data.remote.dto.MarketDto
import com.polypulse.app.domain.model.Market
import com.polypulse.app.domain.model.Outcome
import com.polypulse.app.domain.repository.MarketRepository

class MarketRepositoryImpl(
    private val api: PolymarketApi
) : MarketRepository {

    override suspend fun getMarkets(): Result<List<Market>> {
        return try {
            val response = api.getMarkets()
            val markets = response.data.map { it.toDomain() }
            Result.success(markets)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun MarketDto.toDomain(): Market {
        val domainOutcomes = tokens?.map { token ->
            Outcome(
                name = token.outcome,
                price = token.price ?: 0.0,
                tokenId = token.tokenId
            )
        } ?: emptyList()

        return Market(
            id = conditionId,
            question = question,
            imageUrl = image,
            volume = volume ?: 0.0,
            outcomes = domainOutcomes,
            tags = tags ?: emptyList()
        )
    }
}
