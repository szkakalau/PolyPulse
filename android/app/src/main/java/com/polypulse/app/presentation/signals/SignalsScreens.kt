package com.polypulse.app.presentation.signals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Divider
import com.polypulse.app.R
import com.polypulse.app.data.remote.dto.AnalyticsEventRequest
import com.polypulse.app.data.repository.AnalyticsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun SignalsListScreen(
    viewModel: SignalsListViewModel,
    analyticsRepository: AnalyticsRepository,
    onOpenSignal: (Int) -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToFilterRules: () -> Unit,
    onSaveDefault: () -> Unit
) {
    val state = viewModel.state.value
    val scope = rememberCoroutineScope()
    val saveDefaultMessage = remember { mutableStateOf(false) }

    LaunchedEffect(saveDefaultMessage.value) {
        if (saveDefaultMessage.value) {
            delay(2000)
            saveDefaultMessage.value = false
        }
    }
    LaunchedEffect(state.preferredCategories, state.preferenceFilterEnabled) {
        saveDefaultMessage.value = false
    }

    LaunchedEffect(Unit) {
        scope.launch {
            analyticsRepository.trackEvent(AnalyticsEventRequest("signals_list_view"))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.signals_title), style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onNavigateToPreferences) {
                    Text(stringResource(R.string.signals_action_preferences))
                }
                Button(onClick = { viewModel.refresh() }) {
                    Text(stringResource(R.string.signals_action_refresh))
                }
            }
        }

        val sourceLabel = if (state.preferenceSource == "preferences") {
            stringResource(R.string.signals_source_preferences)
        } else {
            stringResource(R.string.signals_source_onboarding)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val status = if (state.temporaryFilterDisabled) {
                stringResource(R.string.signals_prefs_temp_off)
            } else if (state.preferenceFilterEnabled && state.preferredCategories.isNotEmpty()) {
                stringResource(R.string.signals_prefs_on, sourceLabel)
            } else {
                stringResource(R.string.signals_prefs_off)
            }
            SuggestionChip(
                onClick = {
                    if (state.temporaryFilterDisabled) {
                        viewModel.enableFilter()
                    } else {
                        viewModel.disableFilterOnce()
                    }
                },
                label = { Text(status) }
            )
            TextButton(onClick = {
                if (state.temporaryFilterDisabled) {
                    viewModel.enableFilter()
                } else {
                    viewModel.disableFilterOnce()
                }
            }) {
                Text(
                    if (state.temporaryFilterDisabled) {
                        stringResource(R.string.signals_action_enable)
                    } else {
                        stringResource(R.string.signals_action_off_once)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (state.temporaryFilterDisabled) {
            Text(
                text = stringResource(R.string.signals_off_once_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return
        }

        if (state.error != null) {
            Text(text = state.error, color = MaterialTheme.colorScheme.error)
            return
        }

        if (state.preferredCategories.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    state.preferredCategories.forEach { category ->
                        SuggestionChip(onClick = {}, label = { Text(category) })
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(text = stringResource(R.string.signals_use_prefs), style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = state.preferenceFilterEnabled,
                        onCheckedChange = { viewModel.setPreferenceFilterEnabled(it) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { viewModel.clearPreferenceFilter() },
                        enabled = !state.temporaryFilterDisabled
                    ) {
                        Text(stringResource(R.string.signals_action_clear))
                    }
                    TextButton(onClick = { viewModel.setEverythingPreference() }) {
                        Text(stringResource(R.string.signals_action_everything))
                    }
                    TextButton(
                        onClick = { viewModel.clearAndDisablePreferences() },
                        enabled = !state.temporaryFilterDisabled
                    ) {
                        Text(stringResource(R.string.signals_action_clear_off))
                    }
                    TextButton(onClick = {
                        viewModel.saveCurrentPreferencesAsDefault()
                        saveDefaultMessage.value = true
                        onSaveDefault()
                    }) {
                        Text(stringResource(R.string.signals_action_save_default))
                    }
                }
            }
            val total = state.signals.size
            val filtered = if (state.preferenceFilterEnabled && !state.temporaryFilterDisabled) {
                state.filteredSignals.size
            } else {
                total
            }
            Text(
                text = stringResource(
                    R.string.signals_source_summary,
                    sourceLabel,
                    filtered,
                    total
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (saveDefaultMessage.value) {
                Text(
                    text = stringResource(R.string.signals_saved_default),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(R.string.signals_match_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onNavigateToFilterRules) {
                Text(stringResource(R.string.signals_action_view_filter_rules))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        val visibleSignals = if (state.filteredSignals.isNotEmpty()) state.filteredSignals else state.signals

        if (visibleSignals.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.signals_empty_title))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.clearPreferenceFilter() },
                        enabled = !state.temporaryFilterDisabled
                    ) {
                        Text(stringResource(R.string.signals_action_clear_filters))
                    }
                    TextButton(onClick = { viewModel.resetPreferences() }) {
                        Text(stringResource(R.string.signals_action_reset_prefs))
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(visibleSignals) { signal ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onOpenSignal(signal.id) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val signalSourceLabel = stringResource(sourceLabelRes(signal.evidence?.sourceType))
                            val minutesAgo = timeAgoMinutes(signal.createdAt)
                            val timeAgo = if (minutesAgo == null) {
                                stringResource(R.string.signals_time_just_now)
                            } else {
                                stringResource(R.string.signals_time_minutes_ago, minutesAgo)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = signal.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (signal.locked) {
                                        stringResource(R.string.signals_status_locked)
                                    } else {
                                        stringResource(R.string.signals_status_open)
                                    },
                                    color = if (signal.locked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$signalSourceLabel · $timeAgo",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.signals_tier_label, signal.tierRequired),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (signal.locked) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.signals_locked_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SignalDetailScreen(
    signalId: Int,
    viewModel: SignalDetailViewModel,
    refreshKey: Boolean,
    onConsumeRefresh: () -> Unit,
    onUnlock: () -> Unit,
    onViewCredibility: () -> Unit,
    onFeedback: (String) -> Unit
) {
    val state = viewModel.state.value
    val context = LocalContext.current

    LaunchedEffect(signalId) {
        viewModel.load(signalId)
    }

    LaunchedEffect(refreshKey) {
        if (refreshKey) {
            viewModel.load(signalId)
            onConsumeRefresh()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.signal_detail_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
            return
        }

        if (state.error != null) {
            Text(text = state.error, color = MaterialTheme.colorScheme.error)
            return
        }

        val signal = state.signal ?: return

        Text(text = signal.title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.signals_tier_label, signal.tierRequired))
        Text(text = signal.createdAt, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (signal.locked) {
            Text(text = stringResource(R.string.signal_detail_locked))
            Spacer(modifier = Modifier.height(12.dp))
            if (signal.evidence != null) {
                Text(text = stringResource(R.string.signal_detail_evidence_title))
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = stringResource(R.string.signal_detail_source, signal.evidence.sourceType))
                Text(text = stringResource(R.string.signal_detail_triggered, signal.evidence.triggeredAt))
                Text(text = stringResource(R.string.signal_detail_market, signal.evidence.marketId))
                Text(text = stringResource(R.string.signal_detail_wallet, maskWallet(signal.evidence.makerAddress)))
                if (signal.evidence.evidenceUrl.isNotBlank()) {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(signal.evidence.evidenceUrl))
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.signal_detail_link))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Button(onClick = onUnlock) {
                Text(stringResource(R.string.signal_detail_unlock))
            }
            TextButton(onClick = onViewCredibility) {
                Text(stringResource(R.string.signal_detail_credibility))
            }
        } else {
            val contentLines = (signal.content ?: "").lines().filter { it.isNotBlank() }
            if (contentLines.isNotEmpty()) {
                Text(text = stringResource(R.string.signal_detail_conclusion_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = contentLines.first(), style = MaterialTheme.typography.bodyMedium)
                val recommendations = contentLines.drop(1).take(3)
                if (recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = stringResource(R.string.signal_detail_recommendations_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    recommendations.forEach { item ->
                        Text(text = "• $item", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (signal.evidence != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.signal_detail_evidence_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = stringResource(R.string.signal_detail_source, signal.evidence.sourceType))
                Text(text = stringResource(R.string.signal_detail_triggered, signal.evidence.triggeredAt))
                Text(text = stringResource(R.string.signal_detail_market, signal.evidence.marketId))
                Text(text = stringResource(R.string.signal_detail_wallet, maskWallet(signal.evidence.makerAddress)))
                if (signal.evidence.evidenceUrl.isNotBlank()) {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(signal.evidence.evidenceUrl))
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.signal_detail_link))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.signal_detail_risk_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = stringResource(R.string.signal_detail_risk_text), style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.signal_detail_feedback_title),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { onFeedback("helpful") }) {
                    Text(stringResource(R.string.signal_detail_feedback_helpful))
                }
                TextButton(onClick = { onFeedback("not_helpful") }) {
                    Text(stringResource(R.string.signal_detail_feedback_not_helpful))
                }
                TextButton(onClick = { onFeedback("traded") }) {
                    Text(stringResource(R.string.signal_detail_feedback_traded))
                }
            }
        }
    }
}

@Composable
fun FilterRulesScreen(
    onNavigateBack: () -> Unit,
    onRefreshSignals: () -> Unit,
    onFeedbackMissingKeywords: (String, String) -> Unit,
    onAutoBack: () -> Unit
) {
    val emailState = remember { mutableStateOf("") }
    val notesState = remember { mutableStateOf("") }
    val submittedState = remember { mutableStateOf(false) }
    LaunchedEffect(submittedState.value) {
        if (submittedState.value) {
            delay(2000)
            submittedState.value = false
            onAutoBack()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.filter_rules_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.filter_rules_intro))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.filter_rules_politics))
        Text(text = stringResource(R.string.filter_rules_crypto))
        Text(text = stringResource(R.string.filter_rules_sports))
        Text(text = stringResource(R.string.filter_rules_macro))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.filter_rules_example_match))
        Text(text = stringResource(R.string.filter_rules_example_non_match))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.filter_rules_note))
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = emailState.value,
            onValueChange = { emailState.value = it },
            label = { Text(stringResource(R.string.filter_rules_email_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = notesState.value,
            onValueChange = { notesState.value = it },
            label = { Text(stringResource(R.string.filter_rules_notes_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = {
                onFeedbackMissingKeywords(emailState.value, notesState.value)
                emailState.value = ""
                notesState.value = ""
                submittedState.value = true
            },
            enabled = notesState.value.isNotBlank()
        ) {
            Text(stringResource(R.string.filter_rules_action_report))
        }
        if (submittedState.value) {
            Text(
                text = stringResource(R.string.filter_rules_submitted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onNavigateBack) {
                Text(stringResource(R.string.filter_rules_action_back))
            }
            Button(onClick = {
                onRefreshSignals()
                onNavigateBack()
            }) {
                Text(stringResource(R.string.filter_rules_action_back_refresh))
            }
        }
    }
}

private fun sourceLabelRes(sourceType: String?): Int {
    val normalized = sourceType?.lowercase(Locale.US) ?: ""
    return when {
        normalized.contains("whale") -> R.string.signals_source_whale
        normalized.contains("activity") -> R.string.signals_source_activity
        else -> R.string.signals_source_system
    }
}

private fun timeAgoMinutes(timestamp: String): Int? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )
    val millis = patterns.firstNotNullOfOrNull { pattern ->
        try {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(timestamp)?.time
        } catch (e: Exception) {
            null
        }
    } ?: return null
    val deltaMinutes = ((System.currentTimeMillis() - millis) / 60000).toInt()
    val safeMinutes = if (deltaMinutes < 1) 1 else deltaMinutes
    return safeMinutes
}

private fun maskWallet(address: String): String {
    if (address.length <= 10) return address
    return "${address.take(6)}…${address.takeLast(4)}"
}
