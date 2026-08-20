package by.vsdev.cpt.feature.portfolio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.cpt.core.model.AccountBreakdown
import by.vsdev.cpt.core.model.AssetBreakdown
import by.vsdev.cpt.core.model.ProviderError
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = viewModel::refresh) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(if (state.isRefreshing) "Refreshing…" else "Refresh")
            }
        },
    ) { padding ->
        val snapshot = state.snapshot
        if (snapshot == null || snapshot.byAccount.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No accounts yet — add a wallet, exchange, or custom asset to get started.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total portfolio value")
                        Text("$${formatUsd(snapshot.totalValueUsd)}")
                        snapshot.lastUpdated?.let { Text(formatLastUpdated(it)) }
                    }
                }
                item {
                    Text(
                        "By account",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                items(snapshot.byAccount) { account ->
                    AccountRow(account, error = state.lastErrors[account.accountId.value])
                }
                if (snapshot.byAsset.isNotEmpty()) {
                    item {
                        Text(
                            "By asset",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(snapshot.byAsset) { asset -> AssetRow(asset) }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: AccountBreakdown,
    error: ProviderError?,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(account.displayName)
        Text("$${formatUsd(account.valueUsd)}")
        if (error != null) {
            Text(
                "Sync failed: ${error.message}",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AssetRow(asset: AssetBreakdown) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("${asset.assetSymbol} · ${asset.quantity}")
        Text("$${formatUsd(asset.valueUsd)}")
    }
}

private const val CENTS_PER_DOLLAR = 100
private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24

private fun formatUsd(value: Double): String {
    val rounded =
        (value * CENTS_PER_DOLLAR).let { if (it < 0) kotlin.math.ceil(it) else kotlin.math.floor(it) } / CENTS_PER_DOLLAR
    return rounded.toString()
}

private fun formatLastUpdated(lastUpdated: Instant): String {
    val elapsedSeconds = (Clock.System.now() - lastUpdated).inWholeSeconds.coerceAtLeast(0)
    val elapsedMinutes = elapsedSeconds / SECONDS_PER_MINUTE
    val elapsedHours = elapsedMinutes / MINUTES_PER_HOUR
    val elapsedDays = elapsedHours / HOURS_PER_DAY
    return when {
        elapsedSeconds < SECONDS_PER_MINUTE -> "Updated just now"
        elapsedMinutes < MINUTES_PER_HOUR -> "Updated ${elapsedMinutes}m ago"
        elapsedHours < HOURS_PER_DAY -> "Updated ${elapsedHours}h ago"
        else -> "Updated ${elapsedDays}d ago"
    }
}
