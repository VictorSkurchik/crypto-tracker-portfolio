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
import by.vsdev.cpt.core.model.ProviderError
import org.koin.compose.viewmodel.koinViewModel

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
                    }
                }
                items(snapshot.byAccount) { account ->
                    AccountRow(account, error = state.lastErrors[account.accountId.value])
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

private fun formatUsd(value: Double): String {
    val rounded = (value * 100).let { if (it < 0) kotlin.math.ceil(it) else kotlin.math.floor(it) } / 100
    return rounded.toString()
}
