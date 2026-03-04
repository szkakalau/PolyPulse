package com.polypulse.app.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.polypulse.app.R

@Composable
fun OnboardingScreen(
    onComplete: (enableNotifications: Boolean, whaleRadarEnabled: Boolean, dailyPulseEnabled: Boolean, categories: Set<String>) -> Unit,
    onSkip: () -> Unit
) {
    val stepState = remember { mutableStateOf(0) }
    val enableNotifications = remember { mutableStateOf(true) }
    val whaleRadarEnabled = remember { mutableStateOf(true) }
    val dailyPulseEnabled = remember { mutableStateOf(false) }
    val selectedCategories = remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        when (stepState.value) {
            0 -> {
                Text(text = stringResource(R.string.onboarding_headline_early_moves), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.onboarding_subtitle_early_moves),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { stepState.value = 1 }) {
                        Text(stringResource(R.string.onboarding_action_get_started))
                    }
                    OutlinedButton(onClick = onSkip) {
                        Text(stringResource(R.string.onboarding_action_browse_first))
                    }
                }
            }
            1 -> {
                Text(text = stringResource(R.string.onboarding_headline_markets), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.onboarding_subtitle_markets),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreferenceChip(label = stringResource(R.string.category_politics), selected = selectedCategories.value.contains("Politics")) {
                        toggleCategory(selectedCategories, "Politics")
                    }
                    PreferenceChip(label = stringResource(R.string.category_sports), selected = selectedCategories.value.contains("Sports")) {
                        toggleCategory(selectedCategories, "Sports")
                    }
                    PreferenceChip(label = stringResource(R.string.category_crypto), selected = selectedCategories.value.contains("Crypto")) {
                        toggleCategory(selectedCategories, "Crypto")
                    }
                    PreferenceChip(label = stringResource(R.string.category_macro), selected = selectedCategories.value.contains("Macro")) {
                        toggleCategory(selectedCategories, "Macro")
                    }
                    PreferenceChip(label = stringResource(R.string.category_everything), selected = selectedCategories.value.contains("Everything")) {
                        toggleCategory(selectedCategories, "Everything")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { stepState.value = 2 }) {
                    Text(stringResource(R.string.onboarding_action_next))
                }
            }
            else -> {
                Text(text = stringResource(R.string.onboarding_headline_alerts), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.onboarding_subtitle_alerts),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleRow(
                        title = stringResource(R.string.onboarding_toggle_whale_radar),
                        checked = whaleRadarEnabled.value,
                        onCheckedChange = { whaleRadarEnabled.value = it }
                    )
                    ToggleRow(
                        title = stringResource(R.string.onboarding_toggle_daily_pulse),
                        checked = dailyPulseEnabled.value,
                        onCheckedChange = { dailyPulseEnabled.value = it }
                    )
                    ToggleRow(
                        title = stringResource(R.string.onboarding_toggle_push_notifications),
                        checked = enableNotifications.value,
                        onCheckedChange = { enableNotifications.value = it }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onComplete(enableNotifications.value, whaleRadarEnabled.value, dailyPulseEnabled.value, selectedCategories.value) }) {
                        Text(stringResource(R.string.onboarding_action_finish))
                    }
                    OutlinedButton(onClick = onSkip) { Text(stringResource(R.string.onboarding_action_not_now)) }
                }
            }
        }
    }
}

@Composable
private fun PreferenceChip(label: String, selected: Boolean, onToggle: () -> Unit) {
    if (selected) {
        Button(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Text(label)
        }
    }
}

private fun toggleCategory(selected: androidx.compose.runtime.MutableState<Set<String>>, category: String) {
    selected.value = if (selected.value.contains(category)) {
        selected.value - category
    } else {
        if (category == "Everything") {
            setOf("Everything")
        } else {
            (selected.value - "Everything") + category
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
