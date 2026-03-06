package com.polypulse.app.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polypulse.app.R
import com.polypulse.app.data.repository.AnalyticsRepository
import com.polypulse.app.data.remote.dto.AnalyticsEventRequest
import com.polypulse.app.data.remote.dto.PaywallPlanDto
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    isLoggedIn: Boolean,
    analyticsRepository: AnalyticsRepository,
    preselectedPlanId: String?,
    messageId: String?,
    source: String?,
    userTier: String?,
    onNavigateToLogin: () -> Unit,
    onTrialStarted: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state = viewModel.state.value
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selectedPlanId = remember(state.plans, preselectedPlanId) {
        preselectedPlanId
            ?: state.plans.firstOrNull { it.period.lowercase(Locale.US).contains("year") || it.id.lowercase(Locale.US).contains("annual") }?.id
            ?: state.plans.firstOrNull { it.id != "free" }?.id
    }
    val selectedPlanIdState = remember { mutableStateOf(selectedPlanId) }
    val restoreMessage = remember { mutableStateOf<String?>(null) }
    val restoreLoading = remember { mutableStateOf(false) }
    LaunchedEffect(state.plans, preselectedPlanId) {
        if (preselectedPlanId != null) {
            selectedPlanIdState.value = preselectedPlanId
        } else if (selectedPlanIdState.value == null) {
            selectedPlanIdState.value = state.plans.firstOrNull { it.period.lowercase(Locale.US).contains("year") || it.id.lowercase(Locale.US).contains("annual") }?.id
                ?: state.plans.firstOrNull { it.id != "free" }?.id
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.refreshEntitlements()
            viewModel.refreshBillingStatus()
        }
    }
    LaunchedEffect(Unit, preselectedPlanId, messageId, source, userTier) {
        scope.launch {
            val properties = mutableMapOf<String, String>()
            if (preselectedPlanId != null) {
                properties["preselected_plan_id"] = preselectedPlanId
            }
            if (messageId != null) {
                properties["message_id"] = messageId
            }
            if (source != null) {
                properties["source"] = source
            }
            if (userTier != null) {
                properties["user_tier"] = userTier
            }
            analyticsRepository.trackEvent(
                AnalyticsEventRequest(
                    "paywall_view_client",
                    properties = if (properties.isEmpty()) null else properties
                )
            )
        }
    }

    fun trackDismiss(reason: String, then: () -> Unit) {
        scope.launch {
            val properties = mutableMapOf(
                "reason" to reason
            )
            if (messageId != null) {
                properties["message_id"] = messageId
            }
            if (source != null) {
                properties["source"] = source
            }
            val selectedPlanIdValue = selectedPlanIdState.value
            if (selectedPlanIdValue != null) {
                properties["planId"] = selectedPlanIdValue
            }
            if (userTier != null) {
                properties["user_tier"] = userTier
            }
            analyticsRepository.trackEvent(
                AnalyticsEventRequest(
                    "paywall_dismiss",
                    properties = properties
                )
            )
            then()
        }
    }

    BackHandler {
        trackDismiss("system_back") {
            onNavigateBack()
        }
    }

    val expiresAt = state.entitlements?.expiresAt
    val isTrialExpired = if (expiresAt != null && state.entitlements.tier == "free") {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        val expiryMs = try {
            patterns.firstNotNullOf { pattern ->
                try {
                    SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.parse(expiresAt)?.time
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: NoSuchElementException) {
            null
        }
        if (expiryMs != null) {
            val isExpired = System.currentTimeMillis() > expiryMs
            if (isExpired) {
                LaunchedEffect(Unit) {
                    scope.launch {
                        analyticsRepository.trackEvent(
                            AnalyticsEventRequest(
                                "trial_expired_banner_view",
                                properties = mapOf("expiresAt" to expiresAt)
                            )
                        )
                    }
                }
            }
            isExpired
        } else {
            false
        }
    } else {
        false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.paywall_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (isTrialExpired) {
            Text(text = stringResource(R.string.paywall_trial_expired), color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("• " + stringResource(R.string.paywall_value_latency))
            Text("• " + stringResource(R.string.paywall_value_full_content))
            Text("• " + stringResource(R.string.paywall_value_quota))
        }
        Spacer(modifier = Modifier.height(12.dp))

        val credibility = state.credibility
        if (credibility != null) {
            Text(text = stringResource(R.string.paywall_credibility_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            CredibilityWindow(
                title = stringResource(R.string.paywall_credibility_window_7d),
                window = credibility.window7d
            )
            Spacer(modifier = Modifier.height(8.dp))
            CredibilityWindow(
                title = stringResource(R.string.paywall_credibility_window_30d),
                window = credibility.window30d
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Text(text = stringResource(R.string.paywall_credibility_unavailable), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
        }

        val monthlyPrice = state.plans.firstOrNull { it.period.lowercase(Locale.US).contains("month") }?.price
        state.plans.forEach { plan ->
            PaywallPlanCard(
                plan = plan,
                isSelected = selectedPlanIdState.value == plan.id,
                onSelected = { selectedPlanIdState.value = plan.id },
                monthlyPrice = monthlyPrice
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoggedIn) {
            if (state.entitlements?.tier == "free" && !isTrialExpired) {
                Button(
                    onClick = {
                        scope.launch {
                            val properties = mutableMapOf<String, String>()
                            if (messageId != null) {
                                properties["message_id"] = messageId
                            }
                            if (source != null) {
                                properties["source"] = source
                            }
                            if (userTier != null) {
                                properties["user_tier"] = userTier
                            }
                            analyticsRepository.trackEvent(
                                AnalyticsEventRequest(
                                    "trial_start_click",
                                    properties = if (properties.isEmpty()) null else properties
                                )
                            )
                        }
                        viewModel.startTrial(
                            onSuccess = {
                            scope.launch {
                                val properties = mutableMapOf<String, String>()
                                if (messageId != null) {
                                    properties["message_id"] = messageId
                                }
                                if (source != null) {
                                    properties["source"] = source
                                }
                                if (userTier != null) {
                                    properties["user_tier"] = userTier
                                }
                                analyticsRepository.trackEvent(
                                    AnalyticsEventRequest(
                                        "trial_start_success",
                                        properties = if (properties.isEmpty()) null else properties
                                    )
                                )
                            }
                            onTrialStarted()
                            },
                            onFailure = { errorMessage ->
                                scope.launch {
                                    val properties = mutableMapOf<String, String>()
                                    if (messageId != null) {
                                        properties["message_id"] = messageId
                                    }
                                    if (source != null) {
                                        properties["source"] = source
                                    }
                                    if (userTier != null) {
                                        properties["user_tier"] = userTier
                                    }
                                    properties["error"] = errorMessage
                                    analyticsRepository.trackEvent(
                                        AnalyticsEventRequest(
                                            "trial_start_failed",
                                            properties = properties
                                        )
                                    )
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.paywall_action_start_trial))
                }
            } else {
                Button(
                    onClick = {
                        val planId = selectedPlanIdState.value
                        if (planId != null) {
                            scope.launch {
                                val properties = mutableMapOf(
                                    "planId" to planId
                                )
                                if (messageId != null) {
                                    properties["message_id"] = messageId
                                }
                                if (source != null) {
                                    properties["source"] = source
                                }
                                if (userTier != null) {
                                    properties["user_tier"] = userTier
                                }
                                analyticsRepository.trackEvent(
                                    AnalyticsEventRequest(
                                        "subscribe_click",
                                        properties = properties
                                    )
                                )
                            }
                            viewModel.subscribeStub(
                                planId,
                                onSuccess = {
                                scope.launch {
                                    val properties = mutableMapOf(
                                        "planId" to planId
                                    )
                                    if (messageId != null) {
                                        properties["message_id"] = messageId
                                    }
                                    if (source != null) {
                                        properties["source"] = source
                                    }
                                    if (userTier != null) {
                                        properties["user_tier"] = userTier
                                    }
                                    analyticsRepository.trackEvent(
                                        AnalyticsEventRequest(
                                            "subscribe_success",
                                            properties = properties
                                        )
                                    )
                                }
                                onTrialStarted()
                                },
                                onFailure = { errorMessage ->
                                    scope.launch {
                                        val properties = mutableMapOf(
                                            "planId" to planId,
                                            "error" to errorMessage
                                        )
                                        if (messageId != null) {
                                            properties["message_id"] = messageId
                                        }
                                        if (source != null) {
                                            properties["source"] = source
                                        }
                                        if (userTier != null) {
                                            properties["user_tier"] = userTier
                                        }
                                        analyticsRepository.trackEvent(
                                            AnalyticsEventRequest(
                                                "subscribe_failed",
                                                properties = properties
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedPlanIdState.value != null
                ) {
                    Text(stringResource(R.string.paywall_action_subscribe))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = {
                restoreLoading.value = true
                restoreMessage.value = context.getString(R.string.restore_loading)
                viewModel.restorePurchases(
                    onSuccess = { status ->
                        restoreLoading.value = false
                        val normalized = status.status.lowercase(Locale.US)
                        if (normalized in listOf("active", "trial", "pro", "paid")) {
                            val endAt = status.endAt?.ifBlank { "—" } ?: "—"
                            restoreMessage.value = context.getString(R.string.restore_success, endAt)
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
            }) {
                Text(stringResource(R.string.paywall_action_restore_purchases))
            }
            if (restoreLoading.value) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            restoreMessage.value?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.paywall_action_login))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.paywall_terms_cancel_anytime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = stringResource(R.string.paywall_terms_google_play), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = {
            trackDismiss("button_back") {
                onNavigateBack()
            }
        }) {
            Text(stringResource(R.string.paywall_action_back))
        }
    }
}

@Composable
fun PaywallPlanCard(
    plan: PaywallPlanDto,
    isSelected: Boolean,
    onSelected: () -> Unit,
    monthlyPrice: Double?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = plan.name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.paywall_price_per_period,
                    formatPrice(plan.price, plan.currency),
                    plan.period
                ),
                style = MaterialTheme.typography.titleMedium
            )
            val savings = computeSavingsPercent(plan, monthlyPrice)
            if (savings != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.paywall_save_percent, savings), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CredibilityWindow(
    title: String,
    window: com.polypulse.app.data.remote.dto.SignalCredibilityWindowResponse
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = stringResource(R.string.paywall_credibility_hit_rate, formatPercent(window.hitRate)))
        Text(text = stringResource(R.string.paywall_credibility_sample, window.evaluatedTotal))
        Text(text = stringResource(R.string.paywall_credibility_evidence, formatPercent(window.evidenceRate)))
        Text(text = stringResource(R.string.paywall_credibility_lead_p50, formatSeconds(window.leadP50Seconds)))
        Text(text = stringResource(R.string.paywall_credibility_lead_p90, formatSeconds(window.leadP90Seconds)))
    }
}

private fun computeSavingsPercent(plan: PaywallPlanDto, monthlyPrice: Double?): Int? {
    if (monthlyPrice == null || monthlyPrice == 0.0) return null
    val period = plan.period.lowercase(Locale.US)
    if (!period.contains("year")) return null
    val yearlyFromMonthly = monthlyPrice * 12.0
    if (yearlyFromMonthly <= 0) return null
    val savings = ((1 - plan.price / yearlyFromMonthly) * 100).toInt()
    return if (savings > 0) savings else null
}

private fun formatPrice(amount: Double, currency: String): String {
    val symbol = when (currency.uppercase(Locale.US)) {
        "CNY" -> "¥"
        "USD" -> "$"
        "EUR" -> "€"
        else -> currency
    }
    val formatted = if (amount % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", amount)
    } else {
        String.format(Locale.US, "%.1f", amount)
    }
    return "$symbol$formatted"
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
