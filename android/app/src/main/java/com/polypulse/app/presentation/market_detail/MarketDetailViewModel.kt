package com.polypulse.app.presentation.market_detail

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polypulse.app.data.remote.dto.PricePointDto
import com.polypulse.app.di.AppModule
import com.polypulse.app.domain.model.Market
import kotlinx.coroutines.launch

data class MarketDetailState(
    val isLoading: Boolean = false,
    val priceHistory: List<PricePointDto> = emptyList(),
    val error: String = ""
)

class MarketDetailViewModel(
    private val market: Market
) : ViewModel() {

    private val _state = mutableStateOf(MarketDetailState())
    val state: State<MarketDetailState> = _state

    private val api = AppModule.api

    init {
        fetchPriceHistory()
    }

    private fun fetchPriceHistory() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                // Fetch history for the market (using condition ID or similar as market ID)
                // Note: The API might expect a specific market ID. We use market.id here.
                val response = api.getPriceHistory(marketId = market.id)
                _state.value = MarketDetailState(
                    priceHistory = response.history,
                    isLoading = false
                )
            } catch (e: Exception) {
                // Mock data if API fails (common in development without real API access or if ID format mismatches)
                // In production, handle error properly.
                val mockData = generateMockHistory()
                _state.value = MarketDetailState(
                    priceHistory = mockData,
                    isLoading = false,
                    error = if (e.message?.contains("404") == true) "" else e.message ?: "Error"
                )
            }
        }
    }
    
    private fun generateMockHistory(): List<PricePointDto> {
        val points = mutableListOf<PricePointDto>()
        var price = 0.5
        val now = System.currentTimeMillis() / 1000
        for (i in 0..20) {
            price += (Math.random() - 0.5) * 0.1
            price = price.coerceIn(0.1, 0.9)
            points.add(PricePointDto(t = now - (20 - i) * 86400, p = price))
        }
        return points
    }
}
