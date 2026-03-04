package com.polypulse.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polypulse.app.R
import com.polypulse.app.data.remote.dto.TopMover
import com.polypulse.app.data.remote.dto.SmartWalletDto
import com.polypulse.app.data.remote.dto.WhaleActivityDto
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToSignals: () -> Unit,
    onNavigateToFilterRules: () -> Unit
) {
    val state by viewModel.state
    val restoredMessage = remember { mutableStateOf(false) }

    LaunchedEffect(state.filterRestoredJustNow) {
        if (state.filterRestoredJustNow) {
            restoredMessage.value = true
            delay(2000)
            restoredMessage.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.dashboard_refresh_content_desc))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (state.error == "Please login") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onNavigateToLogin) {
                            Text(stringResource(R.string.dashboard_action_login))
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.stats != null) {
                        item {
                            Text(
                                text = stringResource(R.string.dashboard_overview_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                StatCard(
                                    title = stringResource(R.string.dashboard_alerts_24h),
                                    value = state.stats!!.alerts_24h.toString(),
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                )
                                StatCard(
                                    title = stringResource(R.string.dashboard_watchlist),
                                    value = state.stats!!.watchlist_count.toString(),
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                )
                            }
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.dashboard_login_stats),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(onClick = onNavigateToLogin) {
                                    Text(stringResource(R.string.dashboard_action_login))
                                }
                            }
                        }
                    }

                    if (state.preferredCategories.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val total = state.whaleActivity.size
                                    val filtered = state.filteredWhaleActivity.size
                                    val label = if (state.temporaryFilterDisabled) {
                                        val countdown = if (state.filterRestorePaused) {
                                            stringResource(R.string.dashboard_filter_paused, state.filterRestoreSeconds)
                                        } else if (state.filterRestoreSeconds > 0) {
                                            stringResource(R.string.dashboard_filter_restores_in, state.filterRestoreSeconds)
                                        } else {
                                            stringResource(R.string.dashboard_filter_tap_on)
                                        }
                                        stringResource(R.string.dashboard_filter_off_session, total, countdown)
                                    } else {
                                        val preview = if (state.showAllPreferences) {
                                            state.preferredCategories.joinToString()
                                        } else {
                                            state.preferredCategories.joinToString(limit = 2, truncated = "…")
                                        }
                                        stringResource(R.string.dashboard_filter_filtered_by, preview, filtered, total)
                                    }
                                    Column {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (restoredMessage.value) {
                                            Text(
                                                text = stringResource(R.string.dashboard_filter_restored),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { viewModel.loadData() }) {
                                            Text(stringResource(R.string.dashboard_action_refresh))
                                        }
                                        TextButton(onClick = { viewModel.clearPreferences() }) {
                                            Text(stringResource(R.string.dashboard_action_clear))
                                        }
                                        if (state.temporaryFilterDisabled) {
                                            TextButton(onClick = { viewModel.enableFilter() }) {
                                                Text(stringResource(R.string.dashboard_action_on))
                                            }
                                        } else {
                                            TextButton(onClick = { viewModel.disableFilterOnce() }) {
                                                Text(stringResource(R.string.dashboard_action_off))
                                            }
                                        }
                                        if (state.temporaryFilterDisabled) {
                                            TextButton(onClick = onNavigateToFilterRules) {
                                                Text(stringResource(R.string.dashboard_action_rules))
                                            }
                                            if (state.filterRestoreSeconds > 0) {
                                                TextButton(onClick = {
                                                    if (state.filterRestorePaused) {
                                                        viewModel.resumeFilterRestore()
                                                    } else {
                                                        viewModel.pauseFilterRestore()
                                                    }
                                                }) {
                                                    Text(
                                                        if (state.filterRestorePaused) {
                                                            stringResource(R.string.dashboard_action_resume_timer)
                                                        } else {
                                                            stringResource(R.string.dashboard_action_pause_timer)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        if (!state.temporaryFilterDisabled && state.preferredCategories.size > 2) {
                                            TextButton(onClick = { viewModel.togglePreferencesExpanded() }) {
                                                Text(
                                                    if (state.showAllPreferences) {
                                                        stringResource(R.string.dashboard_action_less)
                                                    } else {
                                                        stringResource(R.string.dashboard_action_more)
                                                    }
                                                )
                                            }
                                        }
                                        TextButton(onClick = onNavigateToPreferences) {
                                            Text(stringResource(R.string.dashboard_action_manage))
                                        }
                                        TextButton(onClick = onNavigateToSignals) {
                                            Text(stringResource(R.string.dashboard_action_signals))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = onNavigateToLeaderboard,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text(stringResource(R.string.dashboard_action_view_leaderboard))
                        }
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                             Icon(
                                Icons.Default.Warning,
                                contentDescription = stringResource(R.string.dashboard_whale_icon_desc),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.dashboard_whale_radar_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    val visibleWhales = if (state.preferredCategories.isNotEmpty() && !state.temporaryFilterDisabled) {
                        state.filteredWhaleActivity
                    } else {
                        state.whaleActivity
                    }

                    if (visibleWhales.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.dashboard_no_large_trades),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(visibleWhales) { whale ->
                            WhaleActivityItem(whale)
                        }
                    }

                    if (state.stats != null) {
                        item {
                            Text(
                                text = stringResource(R.string.dashboard_top_movers_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }

                        items(state.stats!!.top_movers) { mover ->
                            TopMoverItem(mover)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhaleActivityItem(whale: WhaleActivityDto) {
    val isBuy = whale.side.equals("BUY", ignoreCase = true)
    val color = if (isBuy) Color(0xFF4CAF50) else Color(0xFFE53935)
    val icon = if (isBuy) Icons.Default.TrendingUp else Icons.Default.TrendingDown
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = whale.side,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBuy) stringResource(R.string.whale_buy_label) else stringResource(R.string.whale_sell_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale.US).format(whale.value_usd),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = whale.market_question,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                 Text(
                    text = "${whale.outcome} @ ${whale.price}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // You could format timestamp here (e.g., "2m ago")
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun TopMoverItem(mover: TopMover) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = mover.market_question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = mover.outcome,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${if (mover.change > 0) "+" else ""}${String.format("%.2f", mover.change)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (mover.change > 0) Color.Green else Color.Red
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhaleListScreen(
    viewModel: WhaleListViewModel,
    onNavigateToPreferences: () -> Unit,
    onNavigateToSignals: () -> Unit,
    onNavigateToFilterRules: () -> Unit
) {
    val state by viewModel.state
    val restoredMessage = remember { mutableStateOf(false) }

    LaunchedEffect(state.filterRestoredJustNow) {
        if (state.filterRestoredJustNow) {
            restoredMessage.value = true
            delay(2000)
            restoredMessage.value = false
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whales_title)) },
                actions = {
                    IconButton(onClick = { viewModel.loadWhales() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.dashboard_refresh_content_desc))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.preferredCategories.isNotEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val total = state.whales.size
                            val filtered = state.filteredWhales.size
                            val label = if (state.temporaryFilterDisabled) {
                                val countdown = if (state.filterRestorePaused) {
                                    "Paused at ${state.filterRestoreSeconds}s"
                                } else if (state.filterRestoreSeconds > 0) {
                                    "Restores in ${state.filterRestoreSeconds}s"
                                } else {
                                    "Tap On to restore"
                                }
                                "Filter off this session · $total items · $countdown"
                            } else {
                                val preview = if (state.showAllPreferences) {
                                    state.preferredCategories.joinToString()
                                } else {
                                    state.preferredCategories.joinToString(limit = 2, truncated = "…")
                                }
                                "Filtered by: $preview ($filtered/$total)"
                            }
                            Column {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (restoredMessage.value) {
                                    Text(
                                        text = "Filter restored (this time)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.loadWhales() }) {
                                    Text(stringResource(R.string.dashboard_action_refresh))
                                }
                                TextButton(onClick = { viewModel.clearPreferences() }) {
                                    Text(stringResource(R.string.dashboard_action_clear))
                                }
                                if (state.temporaryFilterDisabled) {
                                    TextButton(onClick = { viewModel.enableFilter() }) {
                                        Text(stringResource(R.string.dashboard_action_on))
                                    }
                                } else {
                                    TextButton(onClick = { viewModel.disableFilterOnce() }) {
                                        Text(stringResource(R.string.dashboard_action_off))
                                    }
                                }
                                if (state.temporaryFilterDisabled) {
                                    TextButton(onClick = onNavigateToFilterRules) {
                                        Text(stringResource(R.string.dashboard_action_rules))
                                    }
                                    if (state.filterRestoreSeconds > 0) {
                                        TextButton(onClick = {
                                            if (state.filterRestorePaused) {
                                                viewModel.resumeFilterRestore()
                                            } else {
                                                viewModel.pauseFilterRestore()
                                            }
                                        }) {
                                                    Text(if (state.filterRestorePaused) "Resume timer" else "Pause timer")
                                        }
                                    }
                                }
                                if (!state.temporaryFilterDisabled && state.preferredCategories.size > 2) {
                                    TextButton(onClick = { viewModel.togglePreferencesExpanded() }) {
                                        Text(if (state.showAllPreferences) "Less" else "More")
                                    }
                                }
                                TextButton(onClick = onNavigateToPreferences) {
                                    Text(stringResource(R.string.dashboard_action_manage))
                                }
                                TextButton(onClick = onNavigateToSignals) {
                                    Text(stringResource(R.string.dashboard_action_signals))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            val visibleWhales = if (state.preferredCategories.isNotEmpty() && !state.temporaryFilterDisabled) {
                state.filteredWhales
            } else {
                state.whales
            }
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                visibleWhales.isEmpty() -> {
                    Text(
                        text = "No whale trades yet.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visibleWhales) { whale ->
                            WhaleActivityItem(whale)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMoneyScreen(
    viewModel: SmartMoneyViewModel
) {
    val state by viewModel.state
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.smart_money_title)) },
                actions = {
                    IconButton(onClick = { viewModel.loadSmartMoney() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.wallets.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "No smart wallets yet.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Smart Money isn’t filtered by preferences yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Smart Money isn’t filtered by preferences yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(state.wallets) { wallet ->
                            SmartMoneyItem(wallet)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartMoneyItem(wallet: SmartWalletDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = wallet.address,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale.US).format(wallet.profit),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Win ${(wallet.win_rate * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "ROI ${String.format("%.2f%%", wallet.roi * 100)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${wallet.total_trades} trades",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
