package com.polypulse.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polypulse.app.data.repository.NotificationSettingsRepository
import com.polypulse.app.presentation.auth.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToSignals: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit
) {
    val state = viewModel.state.value

    if (state.isLoggedIn && state.user != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(text = "Email: ${state.user.email}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Member since: ${state.user.created_at}")
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = { viewModel.logout() }) {
                Text("Logout")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onNavigateToPaywall) {
                Text("Upgrade to Pro")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onNavigateToSignals) {
                Text("Signals")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onNavigateToNotificationSettings) {
                Text("Notification Settings")
            }
        }
    } else {
        // If not logged in, we can show a landing for profile or redirect to login
        // For simplicity, let's just show Login Screen here directly or via navigation
        // But since we have separate routes for Login/Register, we should probably redirect
        // However, if Profile is a tab, it should host the content.
        
        // Let's make ProfileScreen act as a container that shows Login if not auth.
        // But LoginScreen has navigation callbacks.
        
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("You are not logged in")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToLogin) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onNavigateToRegister) {
                Text("Create Account")
            }
        }
    }
}

@Composable
fun NotificationSettingsScreen(
    repository: NotificationSettingsRepository,
    onNavigateBack: () -> Unit
) {
    val enabledState = remember { mutableStateOf(true) }
    val isLoading = remember { mutableStateOf(true) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading.value = true
        errorState.value = null
        val result = repository.getSettings()
        result.onSuccess { enabledState.value = it.enabled }
        result.onFailure { errorState.value = it.message ?: "Failed to load settings" }
        isLoading.value = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Notification Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Signal notifications", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Receive push notifications for new signals")
            }
            Switch(
                checked = enabledState.value,
                onCheckedChange = { checked ->
                    val previous = enabledState.value
                    enabledState.value = checked
                    scope.launch {
                        isLoading.value = true
                        errorState.value = null
                        val result = repository.updateSettings(checked)
                        result.onFailure {
                            enabledState.value = previous
                            errorState.value = it.message ?: "Failed to update settings"
                        }
                        isLoading.value = false
                    }
                }
            )
        }

        if (isLoading.value) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (errorState.value != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorState.value ?: "", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNavigateBack) {
            Text("Back")
        }
    }
}

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    isLoggedIn: Boolean,
    onNavigateToLogin: () -> Unit,
    onTrialStarted: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state = viewModel.state.value

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.refreshEntitlements()
            viewModel.refreshBillingStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PolyPulse Pro",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("High-value alerts, low latency, and performance history")
        Spacer(modifier = Modifier.height(24.dp))

        if (state.entitlements != null) {
            Text(text = "Current tier: ${state.entitlements.tier}")
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.billingStatus != null) {
            val bs = state.billingStatus
            Text(text = "Billing status: ${bs.status}")
            if (bs.planId != null) {
                Text(text = "Plan: ${bs.planId}")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (state.error != null) {
            Text(text = state.error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (isLoggedIn) {
            if (state.plans.isNotEmpty()) {
                state.plans
                    .filter { it.id != "free" }
                    .forEach { plan ->
                        Button(
                            onClick = { viewModel.subscribeStub(plan.id, onTrialStarted) },
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${plan.name} ${plan.price} ${plan.currency}/${plan.period}")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
            }
            Button(
                onClick = { viewModel.startTrial(onTrialStarted) },
                enabled = !state.isLoading
            ) {
                Text("Start Trial")
            }
        } else {
            Button(onClick = onNavigateToLogin) {
                Text("Login to start trial")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNavigateBack) {
            Text("Not now")
        }
    }
}
