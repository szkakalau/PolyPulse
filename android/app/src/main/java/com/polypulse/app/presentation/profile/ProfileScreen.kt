package com.polypulse.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polypulse.app.R
import com.polypulse.app.data.repository.NotificationSettingsRepository
import com.polypulse.app.presentation.auth.AuthViewModel
import com.polypulse.app.data.repository.AnalyticsRepository
import com.polypulse.app.data.notifications.NotificationPreferencesStore
import com.polypulse.app.data.notifications.NotificationPreferences
import com.polypulse.app.data.notifications.NotificationThrottleConfig
import com.polypulse.app.presentation.components.MenuValueBanner
import java.util.Locale
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    paywallViewModel: PaywallViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToSignals: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToFaq: () -> Unit,
    onNavigateToPreferences: () -> Unit
) {
    val state = viewModel.state.value
    val paywallState = paywallViewModel.state.value
    val context = LocalContext.current
    val restoreMessage = remember { mutableStateOf<String?>(null) }
    val restoreLoading = remember { mutableStateOf(false) }
    val showValueBanner = rememberSaveable { mutableStateOf(true) }

    if (state.isLoggedIn && state.user != null) {
        val tier = paywallState.entitlements?.tier ?: "free"
        val tierLabel = tier.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        val status = paywallState.billingStatus?.status ?: "unknown"
        val endAt = paywallState.billingStatus?.endAt
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showValueBanner.value) {
                MenuValueBanner(
                    text = stringResource(R.string.menu_value_profile),
                    onDismiss = { showValueBanner.value = false }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(20.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.profile_card_current_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.profile_email_label, state.user.email))
                    Text(text = stringResource(R.string.profile_member_since, state.user.created_at))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.profile_field_plan, tierLabel))
                    Text(text = stringResource(R.string.profile_field_status, status))
                    if (!endAt.isNullOrBlank()) {
                        Text(text = stringResource(R.string.profile_field_expires, endAt))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.profile_card_manage_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onNavigateToPaywall, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.profile_action_upgrade_pro))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/account/subscriptions?package=${context.packageName}")
                        )
                        context.startActivity(intent)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.profile_action_manage_subscription))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        restoreLoading.value = true
                        restoreMessage.value = context.getString(R.string.restore_loading)
                        paywallViewModel.restorePurchases(
                            onSuccess = { billing ->
                                restoreLoading.value = false
                                val normalized = billing.status.lowercase(Locale.US)
                                if (normalized in listOf("active", "trial", "pro", "paid")) {
                                    val dateValue = billing.endAt?.ifBlank { "—" } ?: "—"
                                    restoreMessage.value = context.getString(R.string.restore_success, dateValue)
                                } else if (normalized in listOf("none", "not_found", "missing", "no_purchase")) {
                                    restoreMessage.value = context.getString(R.string.restore_error_no_purchase)
                                } else {
                                    restoreMessage.value = context.getString(R.string.restore_error_network)
                                }
                            },
                            onFailure = {
                                restoreLoading.value = false
                                restoreMessage.value = context.getString(R.string.restore_error_network)
                            }
                        )
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.profile_action_restore_purchases))
                    }
                    if (restoreLoading.value) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    restoreMessage.value?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.profile_card_help_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onNavigateToFaq, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.profile_action_help_faq))
                    }
                    TextButton(onClick = onNavigateToNotificationSettings, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.profile_action_notification_settings))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onNavigateToSignals, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.profile_action_signals))
                }
                OutlinedButton(onClick = onNavigateToPreferences, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.profile_action_preferences))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_action_logout))
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showValueBanner.value) {
                MenuValueBanner(
                    text = stringResource(R.string.menu_value_profile),
                    onDismiss = { showValueBanner.value = false }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(stringResource(R.string.profile_not_logged_in))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToLogin) {
                Text(stringResource(R.string.profile_login))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onNavigateToRegister) {
                Text(stringResource(R.string.profile_register))
            }
        }
    }
}

@Composable
fun NotificationSettingsScreen(
    repository: NotificationSettingsRepository,
    preferencesStore: NotificationPreferencesStore,
    onNavigateBack: () -> Unit
) {
    val enabledState = remember { mutableStateOf(true) }
    val isLoading = remember { mutableStateOf(true) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val preferencesState = remember { mutableStateOf<NotificationPreferences?>(null) }
    val quietStartState = remember { mutableStateOf("22") }
    val quietEndState = remember { mutableStateOf("7") }
    val whaleAlertIntervalState = remember { mutableStateOf("2") }
    val whalePushIntervalState = remember { mutableStateOf("1") }
    val dailyPushIntervalState = remember { mutableStateOf("360") }
    val generalPushIntervalState = remember { mutableStateOf("1") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading.value = true
        errorState.value = null
        val result = repository.getSettings()
        result.onSuccess { enabledState.value = it.enabled }
        result.onFailure { errorState.value = it.message ?: "Failed to load settings" }
        preferencesState.value = preferencesStore.getSnapshot()
        val throttleConfig = preferencesStore.getThrottleConfig()
        whaleAlertIntervalState.value = throttleConfig.whaleAlertIntervalMinutes.toString()
        whalePushIntervalState.value = throttleConfig.whalePushIntervalMinutes.toString()
        dailyPushIntervalState.value = throttleConfig.dailyPushIntervalMinutes.toString()
        generalPushIntervalState.value = throttleConfig.generalPushIntervalMinutes.toString()
        preferencesState.value?.let {
            quietStartState.value = it.quietHoursStart.toString()
            quietEndState.value = it.quietHoursEnd.toString()
        }
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

        preferencesState.value?.let { prefs ->
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Default templates", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Whale Radar")
                Switch(
                    checked = prefs.whaleRadarEnabled,
                    onCheckedChange = { checked ->
                        preferencesState.value = prefs.copy(whaleRadarEnabled = checked)
                        scope.launch { preferencesStore.setWhaleRadarEnabled(checked) }
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Daily Pulse")
                Switch(
                    checked = prefs.dailyPulseEnabled,
                    onCheckedChange = { checked ->
                        preferencesState.value = prefs.copy(dailyPulseEnabled = checked)
                        scope.launch { preferencesStore.setDailyPulseEnabled(checked) }
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Quiet hours", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Enable quiet hours")
                Switch(
                    checked = prefs.quietHoursEnabled,
                    onCheckedChange = { checked ->
                        preferencesState.value = prefs.copy(quietHoursEnabled = checked)
                        scope.launch { preferencesStore.setQuietHoursEnabled(checked) }
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quietStartState.value,
                    onValueChange = { quietStartState.value = it },
                    label = { Text("Start hour") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = quietEndState.value,
                    onValueChange = { quietEndState.value = it },
                    label = { Text("End hour") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = {
                val start = quietStartState.value.toIntOrNull() ?: 22
                val end = quietEndState.value.toIntOrNull() ?: 7
                scope.launch { preferencesStore.setQuietHours(start.coerceIn(0, 23), end.coerceIn(0, 23)) }
            }) {
                Text("Save quiet hours")
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Throttle intervals (minutes)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = whaleAlertIntervalState.value,
                    onValueChange = { whaleAlertIntervalState.value = it },
                    label = { Text("Whale alert") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = whalePushIntervalState.value,
                    onValueChange = { whalePushIntervalState.value = it },
                    label = { Text("Whale push") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = dailyPushIntervalState.value,
                    onValueChange = { dailyPushIntervalState.value = it },
                    label = { Text("Daily push") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = generalPushIntervalState.value,
                    onValueChange = { generalPushIntervalState.value = it },
                    label = { Text("General push") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = {
                val whaleAlert = (whaleAlertIntervalState.value.toIntOrNull() ?: 2).coerceAtLeast(1)
                val whalePush = (whalePushIntervalState.value.toIntOrNull() ?: 1).coerceAtLeast(1)
                val dailyPush = (dailyPushIntervalState.value.toIntOrNull() ?: 360).coerceAtLeast(1)
                val generalPush = (generalPushIntervalState.value.toIntOrNull() ?: 1).coerceAtLeast(1)
                scope.launch {
                    preferencesStore.setThrottleConfig(
                        NotificationThrottleConfig(
                            whaleAlertIntervalMinutes = whaleAlert,
                            whalePushIntervalMinutes = whalePush,
                            dailyPushIntervalMinutes = dailyPush,
                            generalPushIntervalMinutes = generalPush
                        )
                    )
                }
            }) {
                Text("Save throttle")
            }
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
fun FaqScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.faq_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.faq_q_subscription))
        Text(text = stringResource(R.string.faq_a_restore))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.faq_q_cancel))
        Text(text = stringResource(R.string.faq_a_manage))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.faq_q_push))
        Text(text = stringResource(R.string.faq_a_push))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.faq_q_trial))
        Text(text = stringResource(R.string.faq_a_trial))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.faq_q_disclaimer))
        Text(text = stringResource(R.string.faq_a_disclaimer))
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNavigateBack) {
            Text(stringResource(R.string.profile_action_back))
        }
    }
}

@Composable
fun CredibilityScreen(
    viewModel: PaywallViewModel,
    onNavigateBack: () -> Unit
) {
    val state = viewModel.state.value
    LaunchedEffect(Unit) {
        viewModel.loadCredibility()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.paywall_credibility_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        val credibility = state.credibility
        if (credibility == null) {
            Text(text = stringResource(R.string.paywall_credibility_unavailable))
        } else {
            CredibilityWindowBlock(
                title = stringResource(R.string.paywall_credibility_window_7d),
                window = credibility.window7d
            )
            Spacer(modifier = Modifier.height(12.dp))
            CredibilityWindowBlock(
                title = stringResource(R.string.paywall_credibility_window_30d),
                window = credibility.window30d
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNavigateBack) {
            Text(stringResource(R.string.profile_action_back))
        }
    }
}

@Composable
private fun CredibilityWindowBlock(
    title: String,
    window: com.polypulse.app.data.remote.dto.SignalCredibilityWindowResponse
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = stringResource(R.string.paywall_credibility_hit_rate, formatPercent(window.hitRate)))
        Text(text = stringResource(R.string.paywall_credibility_sample, window.evaluatedTotal))
        Text(text = stringResource(R.string.paywall_credibility_evidence, formatPercent(window.evidenceRate)))
        Text(text = stringResource(R.string.paywall_credibility_lead_p50, formatSeconds(window.leadP50Seconds)))
        Text(text = stringResource(R.string.paywall_credibility_lead_p90, formatSeconds(window.leadP90Seconds)))
    }
}

private fun formatPercent(value: Double): String {
    return String.format(Locale.US, "%.1f%%", value * 100)
}

private fun formatSeconds(value: Int?): String {
    if (value == null) return "—"
    return if (value < 60) {
        "${value}s"
    } else {
        val minutes = value / 60
        val seconds = value % 60
        "${minutes}m ${seconds}s"
    }
}

@Composable
fun PreferencesScreen(
    preferencesStore: com.polypulse.app.data.onboarding.OnboardingPreferencesStore,
    onNavigateBack: () -> Unit
) {
    val selected = remember { mutableStateOf(setOf<String>()) }
    val showSaved = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        selected.value = preferencesStore.getPreferredCategories()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Preferences", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Select the markets you care about most.")
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Affects Signals, Whale Radar, and Dashboard lists.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Default preferences come from onboarding.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Changes apply immediately to Signals and Dashboard.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tip: Open Preferences from Signals for quick edits.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showSaved.value) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Saved",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PreferenceRow("Politics", selected.value.contains("Politics")) {
                selected.value = togglePreference(selected.value, "Politics")
            }
            PreferenceRow("Sports", selected.value.contains("Sports")) {
                selected.value = togglePreference(selected.value, "Sports")
            }
            PreferenceRow("Crypto", selected.value.contains("Crypto")) {
                selected.value = togglePreference(selected.value, "Crypto")
            }
            PreferenceRow("Macro", selected.value.contains("Macro")) {
                selected.value = togglePreference(selected.value, "Macro")
            }
            PreferenceRow("Everything", selected.value.contains("Everything")) {
                selected.value = togglePreference(selected.value, "Everything")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                selected.value = setOf("Politics", "Sports", "Crypto", "Macro")
            }) {
                Text("Select All")
            }
            OutlinedButton(onClick = {
                selected.value = emptySet()
            }) {
                Text("Clear All")
            }
            OutlinedButton(onClick = {
                selected.value = setOf("Everything")
            }) {
                Text("Restore Default")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                scope.launch {
                    preferencesStore.setPreferredCategories(selected.value)
                    preferencesStore.setPreferenceSource("preferences")
                    showSaved.value = true
                }
            }) {
                Text("Save")
            }
            OutlinedButton(onClick = {
                scope.launch {
                    preferencesStore.setPreferredCategories(selected.value)
                    preferencesStore.setPreferenceSource("preferences")
                    showSaved.value = true
                    onNavigateBack()
                }
            }) {
                Text("Save & Back")
            }
            if (showSaved.value) {
                OutlinedButton(onClick = { showSaved.value = false }) {
                    Text("Continue Editing")
                }
            }
            OutlinedButton(onClick = onNavigateBack) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun PreferenceRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

private fun togglePreference(current: Set<String>, category: String): Set<String> {
    return if (current.contains(category)) {
        current - category
    } else {
        if (category == "Everything") {
            setOf("Everything")
        } else {
            (current - "Everything") + category
        }
    }
}
