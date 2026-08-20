package by.vsdev.cpt.feature.wallets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.cpt.core.designsystem.CptBadgeShape
import by.vsdev.cpt.core.designsystem.CptChip
import by.vsdev.cpt.core.designsystem.CptCoinBadge
import by.vsdev.cpt.core.designsystem.CptUnderlineTextField
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.ChainId
import org.koin.compose.viewmodel.koinViewModel

private fun ChainId.iconSymbol(): String =
    when (this) {
        ChainId.ETHEREUM -> "ETH"
        ChainId.OPTIMISM -> "OP"
        ChainId.ARBITRUM -> "ARB"
        ChainId.TON -> "TON"
        ChainId.TRON -> "TRX"
    }

@Composable
fun WalletsScreen(viewModel: WalletsViewModel = koinViewModel()) {
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val addressError by viewModel.addressError.collectAsStateWithLifecycle()
    var displayName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var chain by remember { mutableStateOf(ChainId.ETHEREUM) }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            Text(
                "Add wallet",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            )
            Text(
                "Chain",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChainId.entries.forEach { entry ->
                    CptChip(
                        label = entry.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = chain == entry,
                        onClick = {
                            chain = entry
                            viewModel.clearAddressError()
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
                value = address,
                onValueChange = {
                    address = it
                    viewModel.clearAddressError()
                },
                label = { Text("Wallet address") },
                isError = addressError != null,
                supportingText = { addressError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Button(
                onClick = {
                    viewModel.addWallet(displayName, chain, address)
                    if (addressError == null) {
                        displayName = ""
                        address = ""
                    }
                },
                modifier = Modifier.padding(top = 20.dp),
            ) { Text("Add wallet") }

            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CptCoinBadge(wallet.chain.iconSymbol(), CptBadgeShape.CIRCLE, modifier = Modifier.padding(end = 10.dp))
            Column {
                Text(wallet.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${wallet.chain.name}: ${wallet.address}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}
