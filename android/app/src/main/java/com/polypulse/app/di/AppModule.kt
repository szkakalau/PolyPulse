package com.polypulse.app.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.polypulse.app.data.auth.AuthRepository
import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.remote.BackendApi
import com.polypulse.app.data.remote.BackendApiProvider
import com.polypulse.app.data.remote.PolymarketApi
import com.polypulse.app.data.repository.MarketRepositoryImpl
import com.polypulse.app.data.repository.PaywallRepository
import com.polypulse.app.data.repository.SignalsRepository
import com.polypulse.app.data.repository.WatchlistRepository
import com.polypulse.app.data.repository.NotificationSettingsRepository
import com.polypulse.app.domain.repository.MarketRepository
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import retrofit2.Retrofit
import java.net.Inet4Address
import java.net.InetAddress

import com.polypulse.app.data.repository.DashboardRepository
import com.polypulse.app.data.repository.AnalyticsRepository

object AppModule {
    private const val BASE_URL = "https://clob.polymarket.com/"
    private val backendBaseUrls = listOf(
        "https://backend-production-1981.up.railway.app/",
        "http://10.0.2.2:8000/"
    )

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun preferIpv4(addresses: List<InetAddress>): List<InetAddress> {
        val ipv4 = addresses.filterIsInstance<Inet4Address>()
        return if (ipv4.isNotEmpty()) ipv4 else addresses
    }

    private val doh = DnsOverHttps.Builder()
        .client(OkHttpClient.Builder().build())
        .url("https://dns.google/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("8.8.8.8"),
            InetAddress.getByName("8.8.4.4")
        )
        .build()

    private val polymarketClient = OkHttpClient.Builder()
        .dns(object : okhttp3.Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return try {
                    preferIpv4(doh.lookup(hostname))
                } catch (e: Exception) {
                    preferIpv4(okhttp3.Dns.SYSTEM.lookup(hostname))
                }
            }
        })
        .build()

    private val backendClient = OkHttpClient.Builder().build()

    val api: PolymarketApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(polymarketClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PolymarketApi::class.java)
    }

    val backendApiProvider: BackendApiProvider by lazy {
        val apis = backendBaseUrls.map { url ->
            Retrofit.Builder()
                .baseUrl(url)
                .client(backendClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(BackendApi::class.java)
        }
        BackendApiProvider(apis)
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

    @Volatile
    private var signalsRepository: SignalsRepository? = null

    @Volatile
    private var paywallRepository: PaywallRepository? = null

    @Volatile
    private var analyticsRepository: AnalyticsRepository? = null

    @Volatile
    private var notificationSettingsRepository: NotificationSettingsRepository? = null

    fun provideAuthRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(this) {
            val tokenManager = TokenManager(context)
            val repo = AuthRepository(backendApiProvider, tokenManager)
            authRepository = repo
            repo
        }
    }

    fun provideWatchlistRepository(context: Context): WatchlistRepository {
        return watchlistRepository ?: synchronized(this) {
            val tokenManager = TokenManager(context)
            val repo = WatchlistRepository(backendApiProvider, tokenManager)
            watchlistRepository = repo
            repo
        }
    }

    fun provideDashboardRepository(context: Context): DashboardRepository {
        return dashboardRepository ?: synchronized(this) {
            val tokenManager = TokenManager(context)
            val repo = DashboardRepository(backendApiProvider, tokenManager)
            dashboardRepository = repo
            repo
        }
    }

    fun provideSignalsRepository(context: Context): SignalsRepository {
        return signalsRepository ?: synchronized(this) {
            val tokenManager = TokenManager(context)
            val repo = SignalsRepository(backendApiProvider, tokenManager)
            signalsRepository = repo
            repo
        }
    }

    fun providePaywallRepository(context: Context): PaywallRepository {
        return paywallRepository ?: synchronized(this) {
            val tokenManager = TokenManager(context)
            val repo = PaywallRepository(backendApiProvider, tokenManager)
            paywallRepository = repo
            repo
        }
    }

    fun provideAnalyticsRepository(context: Context): AnalyticsRepository {
        return analyticsRepository ?: synchronized(this) {
            val tokenManager = TokenManager(context)
            val repo = AnalyticsRepository(backendApiProvider, tokenManager)
            analyticsRepository = repo
            repo
        }
    }

    fun provideNotificationSettingsRepository(context: Context): NotificationSettingsRepository {
        return notificationSettingsRepository ?: synchronized(this) {
            val tokenManager = TokenManager(context)
            val repo = NotificationSettingsRepository(backendApiProvider, tokenManager)
            notificationSettingsRepository = repo
            repo
        }
    }
}
