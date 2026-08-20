package by.vsdev.cpt.feature.wallets

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.ChainId
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WalletsScreen(viewModel: WalletsViewModel = koinViewModel()) {
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    var displayName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var chain by remember { mutableStateOf(ChainId.ETHEREUM) }
    var chainMenuExpanded by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Add a wallet")
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                TextButton(onClick = { chainMenuExpanded = true }) { Text(chain.name) }
                DropdownMenu(expanded = chainMenuExpanded, onDismissRequest = { chainMenuExpanded = false }) {
                    ChainId.entries.forEach { entry ->
                        DropdownMenuItem(text = { Text(entry.name) }, onClick = {
                            chain = entry
                            chainMenuExpanded = false
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
                value = address,
                onValueChange = { address = it },
                label = { Text("Wallet address") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Button(
                onClick = {
                    viewModel.addWallet(displayName, chain, address)
                    displayName = ""
                    address = ""
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Add wallet") }

            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                items(wallets) { wallet -> WalletRow(wallet, onRemove = { viewModel.removeWallet(wallet.id) }) }
            }
        }
    }
}

@Composable
private fun WalletRow(
    wallet: Account.OnChainWallet,
    onRemove: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column {
            Text(wallet.displayName)
            Text("${wallet.chain.name}: ${wallet.address}")
        }
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}
