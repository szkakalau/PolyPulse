package com.polypulse.app.presentation.market_list

import com.polypulse.app.domain.model.Market

data class MarketListState(
    val isLoading: Boolean = false,
    val markets: List<Market> = emptyList(),
    val filteredMarkets: List<Market> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val error: String = "",
    val watchlistIds: Set<String> = emptySet()
)
