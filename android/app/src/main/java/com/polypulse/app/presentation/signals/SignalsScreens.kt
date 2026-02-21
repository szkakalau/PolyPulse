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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextButton
import androidx.compose.material3.Divider

@Composable
fun SignalsListScreen(
    viewModel: SignalsListViewModel,
    onOpenSignal: (Int) -> Unit
) {
    val state = viewModel.state.value

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
            Text(text = "Signals", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { viewModel.refresh() }) {
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.signals) { signal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onOpenSignal(signal.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = signal.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (signal.locked) "Locked" else "Open",
                                color = if (signal.locked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tier: ${signal.tierRequired}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = signal.createdAt,
                            style = MaterialTheme.typography.bodySmall
                        )
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
    onFeedback: (String) -> Unit
) {
    val state = viewModel.state.value

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
        Text(text = "Signal", style = MaterialTheme.typography.headlineMedium)
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
        Text(text = "Tier: ${signal.tierRequired}")
        Text(text = signal.createdAt, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (signal.locked) {
            Text(text = "This signal is locked.")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onUnlock) {
                Text("Unlock")
            }
        } else {
            Text(text = signal.content ?: "")
            
            // Feedback buttons
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Was this signal helpful?",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { onFeedback("helpful") }) {
                    Text("Helpful")
                }
                TextButton(onClick = { onFeedback("not_helpful") }) {
                    Text("Not Helpful")
                }
                TextButton(onClick = { onFeedback("traded") }) {
                    Text("I Traded")
                }
            }
        }
    }
}

