package by.vsdev.cpt.feature.wallets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.cpt.core.designsystem.CptBadgeShape
import by.vsdev.cpt.core.designsystem.CptChip
import by.vsdev.cpt.core.designsystem.CptCoinBadge
import by.vsdev.cpt.core.designsystem.CptUnderlineTextField
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.ChainId
import crypto_portfolio_tracker.feature.wallets.generated.resources.Res
import crypto_portfolio_tracker.feature.wallets.generated.resources.wallets_add_button
import crypto_portfolio_tracker.feature.wallets.generated.resources.wallets_address_label
import crypto_portfolio_tracker.feature.wallets.generated.resources.wallets_chain_label
import crypto_portfolio_tracker.feature.wallets.generated.resources.wallets_empty_state
import crypto_portfolio_tracker.feature.wallets.generated.resources.wallets_label_optional
import crypto_portfolio_tracker.feature.wallets.generated.resources.wallets_remove_button
import crypto_portfolio_tracker.feature.wallets.generated.resources.wallets_title
import org.jetbrains.compose.resources.stringResource
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
    var displayName by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    // ChainId isn't saveable on every KMP target, so its name is saved instead of the enum itself.
    var chainName by rememberSaveable { mutableStateOf(ChainId.ETHEREUM.name) }
    val chain = remember(chainName) { ChainId.valueOf(chainName) }

    val addWallet: () -> Unit = {
        viewModel.addWallet(displayName, chain, address)
        if (addressError == null) {
            displayName = ""
            address = ""
        }
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            Text(
                stringResource(Res.string.wallets_title),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            )
            Text(
                stringResource(Res.string.wallets_chain_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChainId.entries.forEach { entry ->
                    CptChip(
                        label = entry.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = chain == entry,
                        onClick = {
                            chainName = entry.name
                            viewModel.clearAddressError()
                        },
                    )
                }
            }
            CptUnderlineTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(Res.string.wallets_label_optional)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            CptUnderlineTextField(
                value = address,
                onValueChange = {
                    address = it
                    viewModel.clearAddressError()
                },
                label = { Text(stringResource(Res.string.wallets_address_label)) },
                isError = addressError != null,
                supportingText = { addressError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addWallet() }),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Button(
                onClick = addWallet,
                modifier = Modifier.padding(top = 20.dp),
            ) { Text(stringResource(Res.string.wallets_add_button)) }

            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                if (wallets.isEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.wallets_empty_state),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                } else {
                    items(wallets) { wallet -> WalletRow(wallet, onRemove = { viewModel.removeWallet(wallet.id) }) }
                }
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
        TextButton(onClick = onRemove) { Text(stringResource(Res.string.wallets_remove_button)) }
    }
}
