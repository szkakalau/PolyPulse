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
import androidx.compose.ui.unit.dp
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
    val selectedPlanId = remember(state.plans, preselectedPlanId) {
        preselectedPlanId ?: state.plans.firstOrNull { it.id != "free" }?.id
    }
    val selectedPlanIdState = remember { mutableStateOf(selectedPlanId) }
    LaunchedEffect(state.plans, preselectedPlanId) {
        if (preselectedPlanId != null) {
            selectedPlanIdState.value = preselectedPlanId
        } else if (selectedPlanIdState.value == null) {
            selectedPlanIdState.value = state.plans.firstOrNull { it.id != "free" }?.id
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
        Text(
            text = "Unlock with Pro",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isTrialExpired) {
            Text(
                "Your trial has expired. Upgrade to continue.",
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        state.plans.forEach { plan ->
            PaywallPlanCard(
                plan = plan,
                isSelected = selectedPlanIdState.value == plan.id,
                onSelected = { selectedPlanIdState.value = plan.id }
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
                    Text("Start 7-day free trial")
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
                    Text("Subscribe Now")
                }
            }
        } else {
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login to Start")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = {
            trackDismiss("button_back") {
                onNavigateBack()
            }
        }) {
            Text("Back")
        }
    }
}

@Composable
fun PaywallPlanCard(
    plan: PaywallPlanDto,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    // A simplified card for demonstration
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
            Text(text = "${plan.price}/${plan.period}", style = MaterialTheme.typography.titleMedium)
        }
    }
}
