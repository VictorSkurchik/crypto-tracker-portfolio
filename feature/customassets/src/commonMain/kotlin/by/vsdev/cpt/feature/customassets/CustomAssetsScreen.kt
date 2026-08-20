package by.vsdev.cpt.feature.customassets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import by.vsdev.cpt.core.model.CustomAssetPricing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CustomAssetsScreen(viewModel: CustomAssetsViewModel = koinViewModel()) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val validationError by viewModel.validationError.collectAsStateWithLifecycle()
    var displayName by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var fixedPrice by remember { mutableStateOf("") }
    var useLivePricing by remember { mutableStateOf(false) }
    var cmcSymbol by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Add a custom asset")
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Label (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = symbol,
                onValueChange = {
                    symbol = it
                    viewModel.clearValidationError()
                },
                label = { Text("Symbol (e.g. BTC)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = quantity,
                onValueChange = {
                    quantity = it
                    viewModel.clearValidationError()
                },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Live price from CoinMarketCap")
                Switch(checked = useLivePricing, onCheckedChange = { useLivePricing = it })
            }
            if (useLivePricing) {
                OutlinedTextField(
                    value = cmcSymbol,
                    onValueChange = {
                        cmcSymbol = it
                        viewModel.clearValidationError()
                    },
                    label = { Text("CoinMarketCap symbol") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            } else {
                OutlinedTextField(
                    value = fixedPrice,
                    onValueChange = {
                        fixedPrice = it
                        viewModel.clearValidationError()
                    },
                    label = { Text("Fixed price (USD per unit)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            if (validationError != null) {
                Text(
                    validationError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: 0.0
                    if (useLivePricing) {
                        viewModel.addLivePricedAsset(displayName, symbol, qty, cmcSymbol)
                    } else {
                        viewModel.addFixedPriceAsset(displayName, symbol, qty, fixedPrice.toDoubleOrNull() ?: 0.0)
                    }
                    if (validationError == null) {
                        displayName = ""
                        symbol = ""
                        quantity = ""
                        fixedPrice = ""
                        cmcSymbol = ""
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Add asset") }

            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                items(assets) { asset -> CustomAssetRow(asset, onRemove = { viewModel.removeAsset(asset.id) }) }
            }
        }
    }
}

@Composable
private fun CustomAssetRow(
    asset: Account.CustomAsset,
    onRemove: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column {
            Text(asset.displayName)
            val pricingLabel =
                when (val pricing = asset.pricing) {
                    is CustomAssetPricing.Fixed -> "${asset.quantity} ${asset.assetSymbol} @ $${pricing.unitPriceUsd}"
                    is CustomAssetPricing.LiveFromCoinMarketCap -> "${asset.quantity} ${asset.assetSymbol} (live: ${pricing.cmcSymbol})"
                }
            Text(pricingLabel)
        }
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}
