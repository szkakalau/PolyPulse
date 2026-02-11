package com.polypulse.app.presentation.market_list

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polypulse.app.di.AppModule
import com.polypulse.app.domain.model.Market
import kotlinx.coroutines.launch

class MarketListViewModel : ViewModel() {

    private val _state = mutableStateOf(MarketListState())
    val state: State<MarketListState> = _state

    private val repository = AppModule.repository

    init {
        getMarkets()
    }

    fun refreshMarkets() {
        getMarkets()
    }

    fun onCategorySelected(category: String) {
        _state.value = _state.value.copy(selectedCategory = category)
        updateFilteredMarkets()
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        updateFilteredMarkets()
    }

    private fun updateFilteredMarkets() {
        val currentMarkets = _state.value.markets
        val category = _state.value.selectedCategory
        val query = _state.value.searchQuery

        val filteredByCategory = filterByCategory(currentMarkets, category)
        val finalFiltered = filterByQuery(filteredByCategory, query)

        _state.value = _state.value.copy(filteredMarkets = finalFiltered)
    }

    private fun getMarkets() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.getMarkets()
            result.onSuccess { markets ->
                _state.value = MarketListState(
                    markets = markets,
                    isLoading = false
                )
                updateFilteredMarkets()
            }.onFailure { error ->
                _state.value = MarketListState(
                    error = error.message ?: "An unexpected error occurred",
                    isLoading = false
                )
            }
        }
    }

    private fun filterByQuery(markets: List<Market>, query: String): List<Market> {
        if (query.isBlank()) return markets
        return markets.filter { market ->
            market.question.contains(query, ignoreCase = true)
        }
    }

    private fun filterByCategory(markets: List<Market>, category: String): List<Market> {
        if (category == "All") return markets
        
        return markets.filter { market ->
            val tags = market.tags.map { it.lowercase() }
            val question = market.question.lowercase()
            
            when (category) {
                "Politics" -> {
                    tags.contains("politics") || 
                    tags.contains("elections") || 
                    question.contains("trump") || 
                    question.contains("biden") || 
                    question.contains("election") ||
                    question.contains("president")
                }
                "Crypto" -> {
                    tags.contains("crypto") || 
                    tags.contains("bitcoin") || 
                    tags.contains("ethereum") || 
                    question.contains("bitcoin") || 
                    question.contains("ethereum") || 
                    question.contains("$") ||
                    question.contains("token")
                }
                "Sports" -> {
                    tags.contains("sports") || 
                    tags.contains("nfl") || 
                    tags.contains("nba") || 
                    tags.contains("soccer") ||
                    tags.contains("ufc") ||
                    question.contains("nfl") || 
                    question.contains("nba") || 
                    question.contains("game") ||
                    question.contains("vs")
                }
                else -> true
            }
        }
    }
}
