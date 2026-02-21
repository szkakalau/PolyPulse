package com.polypulse.app

import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.di.AppModule
import com.polypulse.app.domain.model.Market
import com.polypulse.app.presentation.alerts.AlertsScreen
import com.polypulse.app.presentation.auth.AuthViewModel
import com.polypulse.app.presentation.auth.AuthViewModelFactory
import com.polypulse.app.presentation.auth.LoginScreen
import com.polypulse.app.presentation.auth.RegisterScreen
import com.polypulse.app.presentation.market_detail.MarketDetailScreen
import com.polypulse.app.presentation.market_list.MarketListScreen
import com.polypulse.app.presentation.market_list.MarketListViewModel
import com.polypulse.app.presentation.dashboard.DashboardScreen
import com.polypulse.app.presentation.dashboard.DashboardViewModel
import com.polypulse.app.presentation.dashboard.DashboardViewModelFactory
import com.polypulse.app.presentation.dashboard.WhaleListScreen
import com.polypulse.app.presentation.dashboard.WhaleListViewModel
import com.polypulse.app.presentation.dashboard.WhaleListViewModelFactory
import com.polypulse.app.presentation.dashboard.SmartMoneyScreen
import com.polypulse.app.presentation.dashboard.SmartMoneyViewModel
import com.polypulse.app.presentation.dashboard.SmartMoneyViewModelFactory
import com.polypulse.app.presentation.leaderboard.LeaderboardScreen
import com.polypulse.app.presentation.leaderboard.LeaderboardViewModel
import com.polypulse.app.presentation.leaderboard.LeaderboardViewModelFactory
import com.polypulse.app.presentation.profile.ProfileScreen
import com.polypulse.app.presentation.profile.NotificationSettingsScreen
import com.polypulse.app.presentation.profile.PaywallScreen
import com.polypulse.app.presentation.profile.PaywallViewModel
import com.polypulse.app.presentation.profile.PaywallViewModelFactory
import com.polypulse.app.presentation.signals.SignalsListScreen
import com.polypulse.app.presentation.signals.SignalsListViewModel
import com.polypulse.app.presentation.signals.SignalsListViewModelFactory
import com.polypulse.app.presentation.signals.SignalDetailScreen
import com.polypulse.app.presentation.signals.SignalDetailViewModel
import com.polypulse.app.presentation.signals.SignalDetailViewModelFactory
import com.polypulse.app.presentation.util.NotificationHelper
import com.polypulse.app.ui.theme.PolyPulseTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private val pendingSignalId = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this)
        
        // Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        
        val debugToken = intent.getStringExtra("debug_token")
        if (!debugToken.isNullOrBlank()) {
            TokenManager.setDebugTokenOverride(debugToken)
            val tokenManager = TokenManager(applicationContext)
            runBlocking {
                tokenManager.saveToken(debugToken)
            }
        }
        
        setContent {
            PolyPulseTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PolyPulseApp(
                        pendingSignalId = pendingSignalId.value,
                        onConsumePendingSignal = { pendingSignalId.value = null }
                    )
                }
            }
        }

        updatePendingSignalIdFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        updatePendingSignalIdFromIntent(intent)
    }

    private fun updatePendingSignalIdFromIntent(intent: Intent?) {
        if (intent == null) return
        val raw = intent.getStringExtra(EXTRA_SIGNAL_ID)
            ?: intent.extras?.get(EXTRA_SIGNAL_ID)?.toString()
        val parsed = raw?.toIntOrNull()
        if (parsed != null) {
            pendingSignalId.value = parsed
        }
    }

    companion object {
        const val EXTRA_SIGNAL_ID = "signalId"
    }
}

@Composable
fun PolyPulseApp(
    pendingSignalId: Int?,
    onConsumePendingSignal: () -> Unit
) {
    val navController = rememberNavController()
    val viewModel: MarketListViewModel = viewModel()
    
    val context = LocalContext.current
    val authRepository = remember { AppModule.provideAuthRepository(context) }
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
    val dashboardRepository = remember { AppModule.provideDashboardRepository(context) }
    val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(dashboardRepository))
    val leaderboardViewModel: LeaderboardViewModel = viewModel(factory = LeaderboardViewModelFactory(dashboardRepository))
    val whaleListViewModel: WhaleListViewModel = viewModel(factory = WhaleListViewModelFactory(dashboardRepository))
    val smartMoneyViewModel: SmartMoneyViewModel = viewModel(factory = SmartMoneyViewModelFactory(dashboardRepository))

    val signalsRepository = remember { AppModule.provideSignalsRepository(context) }
    val signalsListViewModel: SignalsListViewModel = viewModel(factory = SignalsListViewModelFactory(signalsRepository))
    val signalDetailViewModel: SignalDetailViewModel = viewModel(factory = SignalDetailViewModelFactory(signalsRepository))

    val paywallRepository = remember { AppModule.providePaywallRepository(context) }
    val paywallViewModel: PaywallViewModel = viewModel(factory = PaywallViewModelFactory(paywallRepository))

    val analyticsRepository = remember { AppModule.provideAnalyticsRepository(context) }
    val notificationSettingsRepository = remember { AppModule.provideNotificationSettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Only show bottom bar on top-level screens
            if (currentRoute == "market_list" || currentRoute == "alerts" || currentRoute == "profile" || currentRoute == "dashboard" || currentRoute == "whales" || currentRoute == "smart") {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home_content_desc)) },
                        label = { Text(stringResource(R.string.nav_markets)) },
                        selected = currentRoute == "market_list",
                        onClick = {
                            navController.navigate("market_list") {
                                popUpTo("market_list") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = currentRoute == "dashboard",
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo("market_list") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Warning, contentDescription = "Whales") },
                        label = { Text("Whales") },
                        selected = currentRoute == "whales",
                        onClick = {
                            navController.navigate("whales") {
                                popUpTo("market_list") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Smart Money") },
                        label = { Text("Smart") },
                        selected = currentRoute == "smart",
                        onClick = {
                            navController.navigate("smart") {
                                popUpTo("market_list") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.alerts_content_desc)) },
                        label = { Text(stringResource(R.string.nav_alerts)) },
                        selected = currentRoute == "alerts",
                        onClick = {
                            navController.navigate("alerts") {
                                popUpTo("market_list") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.profile_content_desc)) },
                        label = { Text(stringResource(R.string.nav_profile)) },
                        selected = currentRoute == "profile",
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo("market_list") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        LaunchedEffect(pendingSignalId) {
            if (pendingSignalId != null) {
                navController.navigate("signal_detail/$pendingSignalId") {
                    launchSingleTop = true
                }
                onConsumePendingSignal()
            }
        }
        NavHost(
            navController = navController, 
            startDestination = "market_list",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("market_list") {
                MarketListScreen(
                    viewModel = viewModel,
                    onMarketClick = { market ->
                        val marketJson = Json.encodeToString(market)
                        val encodedJson = URLEncoder.encode(marketJson, StandardCharsets.UTF_8.toString())
                        navController.navigate("market_detail/$encodedJson")
                    }
                )
            }
            composable("alerts") {
                AlertsScreen()
            }
            composable("profile") {
                ProfileScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToRegister = { navController.navigate("register") },
                    onNavigateToPaywall = { navController.navigate("paywall") },
                    onNavigateToSignals = { navController.navigate("signals") },
                    onNavigateToNotificationSettings = { navController.navigate("notification_settings") }
                )
            }
            composable("notification_settings") {
                NotificationSettingsScreen(
                    repository = notificationSettingsRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate("register") },
                    onLoginSuccess = { navController.popBackStack() }
                )
            }
            composable("register") {
                RegisterScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { navController.navigate("login") },
                    onRegisterSuccess = { navController.popBackStack() }
                )
            }
            composable("dashboard") {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToLeaderboard = { navController.navigate("leaderboard") },
                    onNavigateToLogin = { navController.navigate("login") }
                )
            }
            composable("whales") {
                WhaleListScreen(
                    viewModel = whaleListViewModel
                )
            }
            composable("smart") {
                SmartMoneyScreen(
                    viewModel = smartMoneyViewModel
                )
            }
            composable("leaderboard") {
                LeaderboardScreen(
                    viewModel = leaderboardViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("paywall") {
                PaywallScreen(
                    viewModel = paywallViewModel,
                    isLoggedIn = authViewModel.state.value.isLoggedIn,
                    onNavigateToLogin = { navController.navigate("login") },
                    onTrialStarted = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("trial_started", true)
                        navController.popBackStack()
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("signals") {
                SignalsListScreen(
                    viewModel = signalsListViewModel,
                    onOpenSignal = { signalId ->
                        navController.navigate("signal_detail/$signalId")
                    }
                )
            }
            composable(
                route = "signal_detail/{signalId}",
                arguments = listOf(navArgument("signalId") { type = NavType.IntType })
            ) { backStackEntry ->
                val signalId = backStackEntry.arguments?.getInt("signalId") ?: return@composable
                val refreshKeyState = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<Boolean>("trial_started") ?: false
                SignalDetailScreen(
                    signalId = signalId,
                    viewModel = signalDetailViewModel,
                    refreshKey = refreshKeyState,
                    onConsumeRefresh = {
                        navController.currentBackStackEntry?.savedStateHandle?.set("trial_started", false)
                    },
                    onUnlock = { navController.navigate("paywall") },
                    onFeedback = { feedbackType ->
                        coroutineScope.launch {
                            val eventName = when (feedbackType) {
                                "helpful" -> "signal_helpful"
                                "not_helpful" -> "signal_not_helpful"
                                "traded" -> "signal_traded"
                                else -> "signal_feedback_unknown"
                            }
                            val properties = mapOf("signalId" to signalId.toString())
                            analyticsRepository.trackEvent(eventName = eventName, properties = properties)
                        }
                    }
                )
            }
            composable(
                route = "market_detail/{marketJson}",
                arguments = listOf(navArgument("marketJson") { type = NavType.StringType })
            ) { backStackEntry ->
                val marketJson = backStackEntry.arguments?.getString("marketJson")
                if (marketJson != null) {
                    val market = Json.decodeFromString<Market>(marketJson)
                    MarketDetailScreen(market = market, navController = navController)
                }
            }
        }
    }
}
