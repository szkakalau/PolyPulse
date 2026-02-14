package com.polypulse.app.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.polypulse.app.data.auth.AuthRepository
import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.BackendApi
import com.polypulse.app.data.remote.PolymarketApi
import com.polypulse.app.data.repository.MarketRepositoryImpl
import com.polypulse.app.data.repository.WatchlistRepository
import com.polypulse.app.domain.repository.MarketRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

import com.polypulse.app.data.repository.DashboardRepository

object AppModule {
    private const val BASE_URL = "https://clob.polymarket.com/"
    // Production Backend URL (Railway)
    // private const val BACKEND_URL = "https://polypulse-production.up.railway.app/"
    // Local Emulator Backend URL (10.0.2.2 points to host localhost)
    private const val BACKEND_URL = "http://10.0.2.2:8000/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder().build()

    val api: PolymarketApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PolymarketApi::class.java)
    }

    val backendApi: BackendApi by lazy {
        Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendApi::class.java)
    }

    val repository: MarketRepository by lazy {
        MarketRepositoryImpl(api)
    }

    @Volatile
    private var authRepository: AuthRepository? = null

    @Volatile
    private var watchlistRepository: WatchlistRepository? = null

    @Volatile
    private var dashboardRepository: DashboardRepository? = null

    fun provideAuthRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(this) {
            val tokenManager = TokenManager(context)
            val repo = AuthRepository(backendApi, tokenManager)
            authRepository = repo
            repo
        }
    }

    fun provideWatchlistRepository(context: Context): WatchlistRepository {
        return watchlistRepository ?: synchronized(this) {
            val tokenManager = TokenManager(context)
            val repo = WatchlistRepository(backendApi, tokenManager)
            watchlistRepository = repo
            repo
        }
    }

    fun provideDashboardRepository(context: Context): DashboardRepository {
        return dashboardRepository ?: synchronized(this) {
            val tokenManager = TokenManager(context)
            val repo = DashboardRepository(backendApi, tokenManager)
            dashboardRepository = repo
            repo
        }
    }
}
