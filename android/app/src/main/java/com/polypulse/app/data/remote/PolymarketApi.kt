package com.polypulse.app.data.remote

import com.polypulse.app.data.remote.dto.MarketResponse
import com.polypulse.app.data.remote.dto.OrderBookDto
import com.polypulse.app.data.remote.dto.PriceHistoryResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PolymarketApi {
    @GET("markets")
    suspend fun getMarkets(
        @Query("next_cursor") nextCursor: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("active") active: Boolean = true,
        @Query("order") order: String = "volume" // sort by volume
    ): MarketResponse

    @GET("book")
    suspend fun getOrderBook(
        @Query("token_id") tokenId: String
    ): OrderBookDto

    @GET("prices-history")
    suspend fun getPriceHistory(
        @Query("market") marketId: String,
        @Query("interval") interval: String = "1d",
        @Query("fidelity") fidelity: Int = 100 // Limits data points
    ): PriceHistoryResponse
}
