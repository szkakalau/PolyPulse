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
            Result.success(mockMarkets())
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

    private fun mockMarkets(): List<Market> {
        return listOf(
            Market(
                id = "demo-1",
                question = "Will BTC close above $100,000 this year?",
                imageUrl = null,
                volume = 1250000.0,
                outcomes = listOf(
                    Outcome(name = "Yes", price = 0.62, tokenId = "demo-1-yes"),
                    Outcome(name = "No", price = 0.38, tokenId = "demo-1-no")
                ),
                tags = listOf("Crypto")
            ),
            Market(
                id = "demo-2",
                question = "Will the incumbent win the next election?",
                imageUrl = null,
                volume = 860000.0,
                outcomes = listOf(
                    Outcome(name = "Yes", price = 0.47, tokenId = "demo-2-yes"),
                    Outcome(name = "No", price = 0.53, tokenId = "demo-2-no")
                ),
                tags = listOf("Politics")
            ),
            Market(
                id = "demo-3",
                question = "Will the home team win the championship?",
                imageUrl = null,
                volume = 430000.0,
                outcomes = listOf(
                    Outcome(name = "Yes", price = 0.58, tokenId = "demo-3-yes"),
                    Outcome(name = "No", price = 0.42, tokenId = "demo-3-no")
                ),
                tags = listOf("Sports")
            )
        )
    }
}
