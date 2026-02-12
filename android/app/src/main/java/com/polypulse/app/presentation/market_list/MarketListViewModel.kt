package com.polypulse.app.presentation.market_list

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.polypulse.app.di.AppModule
import com.polypulse.app.domain.model.Market
import kotlinx.coroutines.launch

class MarketListViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = mutableStateOf(MarketListState())
    val state: State<MarketListState> = _state

    private val repository = AppModule.repository
    private val watchlistRepository = AppModule.provideWatchlistRepository(application.applicationContext)
    private val authRepository = AppModule.provideAuthRepository(application.applicationContext)

    init {
        getMarkets()
        getWatchlist()
    }

    fun refreshMarkets() {
        getMarkets()
        getWatchlist()
    }

    fun onCategorySelected(category: String) {
        _state.value = _state.value.copy(selectedCategory = category)
        updateFilteredMarkets()
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        updateFilteredMarkets()
    }

    fun toggleWatchlist(marketId: String) {
        viewModelScope.launch {
            if (authRepository.isUserLoggedIn()) {
                val currentWatchlist = _state.value.watchlistIds.toMutableSet()
                if (currentWatchlist.contains(marketId)) {
                    // Remove locally first for optimistic update
                    currentWatchlist.remove(marketId)
                    _state.value = _state.value.copy(watchlistIds = currentWatchlist)
                    
                    val result = watchlistRepository.removeFromWatchlist(marketId)
                    if (result.isFailure) {
                        // Revert on failure
                        currentWatchlist.add(marketId)
                        _state.value = _state.value.copy(watchlistIds = currentWatchlist)
                    }
                } else {
                    // Add locally
                    currentWatchlist.add(marketId)
                    _state.value = _state.value.copy(watchlistIds = currentWatchlist)
                    
                    val result = watchlistRepository.addToWatchlist(marketId)
                    if (result.isFailure) {
                        // Revert on failure
                        currentWatchlist.remove(marketId)
                        _state.value = _state.value.copy(watchlistIds = currentWatchlist)
                    }
                }
            } else {
                // TODO: Handle guest user (maybe prompt login or local storage)
            }
        }
    }

    private fun updateFilteredMarkets() {
        val currentMarkets = _state.value.markets
        val category = _state.value.selectedCategory
        val query = _state.value.searchQuery
        
        val filteredByCategory = if (category == "Watchlist") {
             currentMarkets.filter { _state.value.watchlistIds.contains(it.id) }
        } else {
             filterByCategory(currentMarkets, category)
        }
        
        val finalFiltered = filterByQuery(filteredByCategory, query)

        _state.value = _state.value.copy(filteredMarkets = finalFiltered)
    }

    private fun getMarkets() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.getMarkets()
            result.onSuccess { markets ->
                // Keep the existing watchlistIds and selectedCategory when markets reload
                _state.value = _state.value.copy(
                    markets = markets,
                    isLoading = false
                )
                updateFilteredMarkets()
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    error = error.message ?: "An unexpected error occurred",
                    isLoading = false
                )
            }
        }
    }
    
    private fun getWatchlist() {
        viewModelScope.launch {
            if (authRepository.isUserLoggedIn()) {
                val result = watchlistRepository.getWatchlist()
                result.onSuccess { ids ->
                    _state.value = _state.value.copy(watchlistIds = ids.toSet())
                    // If current tab is Watchlist, refresh the list
                    if (_state.value.selectedCategory == "Watchlist") {
                        updateFilteredMarkets()
                    }
                }
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
                    tags.any { it.contains("politic") || it.contains("election") } || 
                    question.contains("trump", ignoreCase = true) || 
                    question.contains("biden", ignoreCase = true) || 
                    question.contains("election", ignoreCase = true) ||
                    question.contains("president", ignoreCase = true)
                }
                "Crypto" -> {
                    tags.any { it.contains("crypto") || it.contains("bitcoin") || it.contains("ethereum") } || 
                    question.contains("bitcoin", ignoreCase = true) || 
                    question.contains("ethereum", ignoreCase = true) || 
                    question.contains("token", ignoreCase = true)
                }
                "Sports" -> {
                    tags.any { it.contains("sport") || it.contains("nfl") || it.contains("nba") || it.contains("soccer") }
                }
                else -> true
            }
        }
    }
}
