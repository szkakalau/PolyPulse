package com.polypulse.app.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.polypulse.app.data.auth.AuthRepository
import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.BackendApi
import com.polypulse.app.data.remote.PolymarketApi
import com.polypulse.app.data.repository.MarketRepositoryImpl
import com.polypulse.app.domain.repository.MarketRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object AppModule {
    private const val BASE_URL = "https://clob.polymarket.com/"
    // Production Backend URL (Railway)
    private const val BACKEND_URL = "https://polypulse-production.up.railway.app/"

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

    fun provideAuthRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(this) {
            val tokenManager = TokenManager(context)
            val repo = AuthRepository(backendApi, tokenManager)
            authRepository = repo
            repo
        }
    }
}
