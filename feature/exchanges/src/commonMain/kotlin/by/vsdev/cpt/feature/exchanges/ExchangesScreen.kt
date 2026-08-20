package by.vsdev.cpt.feature.exchanges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.cpt.core.designsystem.CptBadgeShape
import by.vsdev.cpt.core.designsystem.CptChip
import by.vsdev.cpt.core.designsystem.CptCoinBadge
import by.vsdev.cpt.core.designsystem.CptUnderlineTextField
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.ExchangeId
import org.koin.compose.viewmodel.koinViewModel

private fun ExchangeId.iconSymbol(): String =
    when (this) {
        ExchangeId.BINANCE -> "BN"
        ExchangeId.OKX -> "OK"
        ExchangeId.BYBIT -> "BY"
        ExchangeId.BITGET -> "BG"
    }

@Composable
fun ExchangesScreen(viewModel: ExchangesViewModel = koinViewModel()) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val connectState by viewModel.connectState.collectAsStateWithLifecycle()
    var displayName by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var apiSecret by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var exchange by remember { mutableStateOf(ExchangeId.BINANCE) }

    var wasVerifying by remember { mutableStateOf(false) }
    LaunchedEffect(connectState) {
        if (wasVerifying && !connectState.isVerifying && connectState.connectionError == null) {
            displayName = ""
            apiKey = ""
            apiSecret = ""
            passphrase = ""
        }
        wasVerifying = connectState.isVerifying
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            Text("Add exchange", style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp))
            Text(
                "Read-only API key recommended — never grant withdrawal permission.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            )
            Text(
                "Exchange",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExchangeId.entries.forEach { entry ->
                    CptChip(
                        label = entry.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = exchange == entry,
                        onClick = {
                            exchange = entry
                            viewModel.clearConnectionError()
                        },
                    )
                }
            }
            CptUnderlineTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Label (optional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            CptUnderlineTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    viewModel.clearConnectionError()
                },
                label = { Text("API key") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            CptUnderlineTextField(
                value = apiSecret,
                onValueChange = {
                    apiSecret = it
                    viewModel.clearConnectionError()
                },
                label = { Text("API secret") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (exchange.requiresPassphrase()) {
                CptUnderlineTextField(
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        viewModel.clearConnectionError()
                    },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            if (connectState.connectionError != null) {
                Text(
                    "Connection failed: ${connectState.connectionError}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Button(
                onClick = { viewModel.addAccount(displayName, exchange, apiKey, apiSecret, passphrase) },
                enabled = !connectState.isVerifying,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                if (connectState.isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                }
                Text(if (connectState.isVerifying) "Verifying…" else "Connect")
            }

            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                items(accounts) { account ->
                    ExchangeAccountRow(account, onRemove = { viewModel.removeAccount(account.id, account.credentialsRef) })
                }
            }
        }
    }
}

@Composable
private fun ExchangeAccountRow(
    account: Account.ExchangeAccount,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CptCoinBadge(
                account.exchange.iconSymbol(),
                CptBadgeShape.SQUARE,
                modifier = Modifier.padding(end = 10.dp),
            )
            Column {
                Text(account.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    account.exchange.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}
