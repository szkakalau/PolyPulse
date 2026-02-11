package com.polypulse.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.polypulse.app.data.remote.PolymarketApi
import com.polypulse.app.data.repository.MarketRepositoryImpl
import com.polypulse.app.domain.repository.MarketRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

import com.polypulse.app.data.remote.BackendApi

object AppModule {
    private const val BASE_URL = "https://clob.polymarket.com/"
    // 10.0.2.2 is localhost for Android Emulator
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
}
