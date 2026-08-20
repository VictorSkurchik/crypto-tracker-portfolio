package by.vsdev.cpt.feature.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.cpt.core.designsystem.CptBadgeShape
import by.vsdev.cpt.core.designsystem.CptCoinBadge
import by.vsdev.cpt.core.model.AccountBreakdown
import by.vsdev.cpt.core.model.AssetBreakdown
import by.vsdev.cpt.core.model.ProviderError
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant

private val chainBadgeIconSymbols =
    mapOf(
        "ETHEREUM" to "ETH",
        "OPTIMISM" to "OP",
        "ARBITRUM" to "ARB",
        "TON" to "TON",
        "TRON" to "TRX",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        val snapshot = state.snapshot
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = viewModel::refresh),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                "Total portfolio value",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(11.dp).padding(end = 6.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                                Text(
                                    if (state.isRefreshing) {
                                        "Refreshing…"
                                    } else {
                                        snapshot?.lastUpdated?.let { formatLastUpdated(it) } ?: "Not updated yet"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            "$${formatUsd(snapshot?.totalValueUsd ?: 0.0)}",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (snapshot == null || snapshot.byAccount.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("No accounts yet — add a wallet, exchange, or custom asset to get started.")
                        }
                    }
                } else {
                    item {
                        Text(
                            "By account",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp),
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        items(snapshot.byAsset) { asset -> AssetRow(asset) }
                    }
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
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val badgeShape = if (account.badge == "EXCHANGE") CptBadgeShape.SQUARE else CptBadgeShape.CIRCLE
                CptCoinBadge(
                    chainBadgeIconSymbols[account.badge] ?: account.badge,
                    badgeShape,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(account.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    account.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .padding(start = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            Text("$${formatUsd(account.valueUsd)}", style = MaterialTheme.typography.bodyLarge)
        }
        if (error != null) {
            Text(
                "Sync failed: ${error.message}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun AssetRow(asset: AssetBreakdown) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CptCoinBadge(asset.assetSymbol, CptBadgeShape.CIRCLE, modifier = Modifier.padding(end = 8.dp))
            Text("${asset.assetSymbol} · ${asset.quantity}", style = MaterialTheme.typography.bodyLarge)
        }
        Text("$${formatUsd(asset.valueUsd)}", style = MaterialTheme.typography.bodyLarge)
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
