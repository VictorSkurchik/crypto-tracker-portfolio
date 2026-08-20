package by.vsdev.cpt.feature.exchanges

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.ExchangeId
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExchangesScreen(viewModel: ExchangesViewModel = koinViewModel()) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var displayName by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var apiSecret by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var exchange by remember { mutableStateOf(ExchangeId.BINANCE) }
    var exchangeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Connect an exchange (read-only API key recommended — never grant withdrawal permission)")
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                TextButton(onClick = { exchangeMenuExpanded = true }) { Text(exchange.name) }
                DropdownMenu(expanded = exchangeMenuExpanded, onDismissRequest = { exchangeMenuExpanded = false }) {
                    ExchangeId.entries.forEach { entry ->
                        DropdownMenuItem(text = { Text(entry.name) }, onClick = {
                            exchange = entry
                            exchangeMenuExpanded = false
                        })
                    }
                }
            }
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Label (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API key") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = apiSecret,
                onValueChange = { apiSecret = it },
                label = { Text("API secret") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (exchange.requiresPassphrase()) {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            Button(
                onClick = {
                    viewModel.addAccount(displayName, exchange, apiKey, apiSecret, passphrase)
                    displayName = ""
                    apiKey = ""
                    apiSecret = ""
                    passphrase = ""
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Connect") }

            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
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
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column {
            Text(account.displayName)
            Text(account.exchange.name)
        }
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}
