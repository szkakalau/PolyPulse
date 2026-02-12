package com.polypulse.app

import android.content.pm.PackageManager
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.polypulse.app.presentation.profile.ProfileScreen
import com.polypulse.app.presentation.util.NotificationHelper
import com.polypulse.app.ui.theme.PolyPulseTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
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
        
        setContent {
            PolyPulseTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PolyPulseApp()
                }
            }
        }
    }
}

@Composable
fun PolyPulseApp() {
    val navController = rememberNavController()
    val viewModel: MarketListViewModel = viewModel()
    
    val context = LocalContext.current
    val authRepository = remember { AppModule.provideAuthRepository(context) }
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Only show bottom bar on top-level screens
            if (currentRoute == "market_list" || currentRoute == "alerts" || currentRoute == "profile") {
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
                    onNavigateToRegister = { navController.navigate("register") }
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
