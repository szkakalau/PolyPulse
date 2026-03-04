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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.polypulse.app.presentation.profile.FaqScreen
import com.polypulse.app.presentation.profile.PreferencesScreen
import com.polypulse.app.presentation.profile.CredibilityScreen
import com.polypulse.app.presentation.profile.PaywallScreen
import com.polypulse.app.presentation.profile.PaywallViewModel
import com.polypulse.app.presentation.profile.PaywallViewModelFactory
import com.polypulse.app.presentation.signals.SignalsListScreen
import com.polypulse.app.presentation.signals.SignalsListViewModel
import com.polypulse.app.presentation.signals.SignalsListViewModelWithPrefsFactory
import com.polypulse.app.presentation.signals.SignalDetailScreen
import com.polypulse.app.presentation.signals.FilterRulesScreen
import com.polypulse.app.presentation.signals.SignalDetailViewModel
import com.polypulse.app.presentation.signals.SignalDetailViewModelFactory
import com.polypulse.app.presentation.onboarding.OnboardingScreen
import com.polypulse.app.presentation.util.NotificationHelper
import com.polypulse.app.presentation.inapp.InAppMessageDialog
import com.polypulse.app.presentation.inapp.InAppMessageViewModel
import com.polypulse.app.presentation.inapp.InAppMessageViewModelFactory
import com.polypulse.app.ui.theme.PolyPulseTheme
import com.polypulse.app.data.remote.dto.AnalyticsEventRequest
import com.polypulse.app.data.notifications.NotificationPreferencesStore
import com.polypulse.app.data.onboarding.OnboardingPreferencesStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import android.net.Uri

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
            ?: intent.extras?.getString(EXTRA_SIGNAL_ID)
        val fromExtra = raw?.toIntOrNull()
        val fromDeepLink = parseSignalIdFromUri(intent.data)
        val resolved = fromExtra ?: fromDeepLink
        if (resolved != null) pendingSignalId.value = resolved
    }

    private fun parseSignalIdFromUri(uri: Uri?): Int? {
        if (uri == null) return null
        val scheme = uri.scheme ?: return null
        if (scheme != "polypulse") return null
        val host = uri.host
        val segments = uri.pathSegments ?: emptyList()
        val candidate = when {
            host == "signals" && segments.isNotEmpty() -> segments.first()
            segments.size >= 2 && segments[0] == "signals" -> segments[1]
            else -> null
        }
        return candidate?.toIntOrNull()
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
    val onboardingStore = remember { AppModule.provideOnboardingStore(context) }
    val onboardingPreferencesStore = remember { AppModule.provideOnboardingPreferencesStore(context) }
    val notificationPreferencesStore = remember { AppModule.provideNotificationPreferencesStore(context) }
    val onboardingCompletedState = remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        onboardingCompletedState.value = onboardingStore.onboardingCompleted.first()
    }
    val dashboardRepository = remember { AppModule.provideDashboardRepository(context) }
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(dashboardRepository, onboardingPreferencesStore)
    )
    val leaderboardViewModel: LeaderboardViewModel = viewModel(factory = LeaderboardViewModelFactory(dashboardRepository))
    val whaleListViewModel: WhaleListViewModel = viewModel(
        factory = WhaleListViewModelFactory(dashboardRepository, onboardingPreferencesStore)
    )
    val smartMoneyViewModel: SmartMoneyViewModel = viewModel(factory = SmartMoneyViewModelFactory(dashboardRepository))

    val signalsRepository = remember { AppModule.provideSignalsRepository(context) }
    val signalsListViewModel: SignalsListViewModel = viewModel(
        factory = SignalsListViewModelWithPrefsFactory(signalsRepository, onboardingPreferencesStore)
    )
    val signalDetailViewModel: SignalDetailViewModel = viewModel(factory = SignalDetailViewModelFactory(signalsRepository))

    val paywallRepository = remember { AppModule.providePaywallRepository(context) }
    val paywallViewModel: PaywallViewModel = viewModel(factory = PaywallViewModelFactory(paywallRepository))

    val inAppMessageRepository = remember { AppModule.provideInAppMessageRepository(context) }
    val inAppMessageStore = remember { AppModule.provideInAppMessageStore(context) }
    val analyticsRepository = remember { AppModule.provideAnalyticsRepository(context) }
    val inAppMessageViewModel: InAppMessageViewModel = viewModel(factory = InAppMessageViewModelFactory(inAppMessageRepository, analyticsRepository, inAppMessageStore))

    val notificationSettingsRepository = remember { AppModule.provideNotificationSettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        inAppMessageViewModel.checkForMessage()
    }

    val inAppMessage by inAppMessageViewModel.inAppMessage.collectAsState()
    inAppMessage?.let { message ->
        InAppMessageDialog(
            message = message,
            onDismiss = { inAppMessageViewModel.onMessageDismissed() },
            onCtaClick = { action ->
                val userTier = paywallViewModel.state.value.entitlements?.tier
                inAppMessageViewModel.onCtaClicked(message, userTier)
                if (action == "open_paywall") {
                    val preselectedPlanId = message.plans
                        ?.firstOrNull { it.id != "free" }
                        ?.id
                    if (preselectedPlanId != null) {
                        navController.navigate("paywall?planId=$preselectedPlanId&messageId=${message.id}&source=in_app_message")
                    } else {
                        navController.navigate("paywall?messageId=${message.id}&source=in_app_message")
                    }
                }
            }
        )
    }
    
    if (onboardingCompletedState.value == null) {
        return
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Only show bottom bar on top-level screens
            if (currentRoute == "market_list" || currentRoute == "alerts" || currentRoute == "profile" || currentRoute == "dashboard" || currentRoute == "whales") {
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
                        label = { Text(stringResource(R.string.nav_insights)) },
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
                coroutineScope.launch {
                    analyticsRepository.trackEvent(
                        AnalyticsEventRequest(
                            "push_open",
                            properties = mapOf("signalId" to pendingSignalId.toString())
                        )
                    )
                }
                navController.navigate("signal_detail/$pendingSignalId") {
                    launchSingleTop = true
                }
                onConsumePendingSignal()
            }
        }
        val startDestination = if (onboardingCompletedState.value == true) "market_list" else "onboarding"
        NavHost(
            navController = navController, 
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("onboarding") {
                OnboardingScreen(
                    onComplete = { enableNotifications, whaleRadarEnabled, dailyPulseEnabled, categories ->
                        coroutineScope.launch {
                            onboardingStore.setOnboardingCompleted(true)
                            notificationPreferencesStore.setWhaleRadarEnabled(whaleRadarEnabled)
                            notificationPreferencesStore.setDailyPulseEnabled(dailyPulseEnabled)
                            onboardingPreferencesStore.setPreferredCategories(categories)
                            onboardingPreferencesStore.setPreferenceSource("onboarding")
                            if (enableNotifications && authViewModel.state.value.isLoggedIn) {
                                notificationSettingsRepository.updateSettings(true)
                            }
                            navController.navigate("market_list") {
                                popUpTo("onboarding") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onSkip = {
                        coroutineScope.launch {
                            onboardingStore.setOnboardingCompleted(true)
                            navController.navigate("market_list") {
                                popUpTo("onboarding") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
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
                AlertsScreen(
                    onNavigateToLogin = { navController.navigate("login") }
                )
            }
            composable("profile") {
                ProfileScreen(
                    viewModel = authViewModel,
                    paywallViewModel = paywallViewModel,
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToRegister = { navController.navigate("register") },
                    onNavigateToPaywall = { navController.navigate("paywall") },
                    onNavigateToSignals = { navController.navigate("signals") },
                    onNavigateToNotificationSettings = { navController.navigate("notification_settings") },
                    onNavigateToFaq = { navController.navigate("faq") },
                    onNavigateToPreferences = { navController.navigate("preferences") }
                )
            }
            composable("notification_settings") {
                NotificationSettingsScreen(
                    repository = notificationSettingsRepository,
                    preferencesStore = notificationPreferencesStore,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("faq") {
                FaqScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("preferences") {
                PreferencesScreen(
                    preferencesStore = onboardingPreferencesStore,
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
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToPreferences = { navController.navigate("preferences") },
                    onNavigateToSignals = { navController.navigate("signals") },
                    onNavigateToFilterRules = { navController.navigate("filter_rules") }
                )
            }
            composable("whales") {
                WhaleListScreen(
                    viewModel = whaleListViewModel,
                    onNavigateToPreferences = { navController.navigate("preferences") },
                    onNavigateToSignals = { navController.navigate("signals") },
                    onNavigateToFilterRules = { navController.navigate("filter_rules") }
                )
            }
            composable("smart") {
                SmartMoneyScreen(
                    viewModel = smartMoneyViewModel
                )
            }
            composable("leaderboard") {
                LeaderboardScreen(
                    viewModel = leaderboardViewModel
                )
            }
            composable(
                route = "paywall?planId={planId}&messageId={messageId}&source={source}",
                arguments = listOf(
                    navArgument("planId") { type = NavType.StringType; nullable = true },
                    navArgument("messageId") { type = NavType.StringType; nullable = true },
                    navArgument("source") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val planId = backStackEntry.arguments?.getString("planId")
                val messageId = backStackEntry.arguments?.getString("messageId")
                val source = backStackEntry.arguments?.getString("source")
                PaywallScreen(
                    viewModel = paywallViewModel,
                    isLoggedIn = authViewModel.state.value.isLoggedIn,
                    analyticsRepository = analyticsRepository,
                    preselectedPlanId = planId,
                    messageId = messageId,
                    source = source,
                    userTier = paywallViewModel.state.value.entitlements?.tier,
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
                    analyticsRepository = analyticsRepository,
                    onOpenSignal = { signalId ->
                        coroutineScope.launch {
                            analyticsRepository.trackEvent(
                                AnalyticsEventRequest(
                                    "signal_open",
                                    properties = mapOf("signalId" to signalId.toString())
                                )
                            )
                        }
                        navController.navigate("signal_detail/$signalId")
                    },
                    onNavigateToPreferences = { navController.navigate("preferences") },
                    onNavigateToFilterRules = { navController.navigate("filter_rules") },
                    onSaveDefault = {
                        coroutineScope.launch {
                            analyticsRepository.trackEvent(
                                AnalyticsEventRequest("signals_save_default")
                            )
                        }
                    }
                )
            }
            composable("filter_rules") {
                FilterRulesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRefreshSignals = { signalsListViewModel.refresh() },
                    onFeedbackMissingKeywords = { email, notes ->
                        coroutineScope.launch {
                            val properties = mutableMapOf<String, String>()
                            if (email.isNotBlank()) {
                                properties["email"] = email
                            }
                            if (notes.isNotBlank()) {
                                properties["notes"] = notes
                            }
                            analyticsRepository.trackEvent(
                                AnalyticsEventRequest(
                                    "filter_rules_missing_keyword",
                                    properties = if (properties.isEmpty()) null else properties
                                )
                            )
                        }
                    },
                    onAutoBack = { navController.popBackStack() }
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
                    onUnlock = {
                        coroutineScope.launch {
                            analyticsRepository.trackEvent(
                                AnalyticsEventRequest(
                                    "signal_unlock_click",
                                    properties = mapOf("signalId" to signalId.toString())
                                )
                            )
                        }
                        navController.navigate("paywall?source=signal_locked")
                    },
                    onViewCredibility = {
                        navController.navigate("credibility")
                    },
                    onFeedback = { feedbackType ->
                        coroutineScope.launch {
                            val eventName = when (feedbackType) {
                                "helpful" -> "signal_helpful"
                                "not_helpful" -> "signal_not_helpful"
                                "traded" -> "signal_traded"
                                else -> "signal_feedback_unknown"
                            }
                            analyticsRepository.trackEvent(
                                AnalyticsEventRequest(
                                    eventName,
                                    properties = mapOf("signalId" to signalId.toString())
                                )
                            )
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
            composable("credibility") {
                CredibilityScreen(
                    viewModel = paywallViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
