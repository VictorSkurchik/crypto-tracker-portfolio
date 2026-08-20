package by.vsdev.cpt.feature.customassets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.cpt.core.designsystem.CptBadgeShape
import by.vsdev.cpt.core.designsystem.CptCoinBadge
import by.vsdev.cpt.core.designsystem.CptUnderlineTextField
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.CustomAssetPricing
import crypto_portfolio_tracker.feature.customassets.generated.resources.Res
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_add_button
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_cmc_symbol_field
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_empty_state
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_fixed_price_field
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_fixed_pricing_summary
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_label_field
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_live_price_switch_label
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_live_pricing_summary
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_quantity_field
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_remove_button
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_symbol_field
import crypto_portfolio_tracker.feature.customassets.generated.resources.customassets_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CustomAssetsScreen(viewModel: CustomAssetsViewModel = koinViewModel()) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val validationError by viewModel.validationError.collectAsStateWithLifecycle()
    var displayName by rememberSaveable { mutableStateOf("") }
    var symbol by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var fixedPrice by rememberSaveable { mutableStateOf("") }
    var useLivePricing by rememberSaveable { mutableStateOf(false) }
    var cmcSymbol by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val submit = {
        val qty = quantity.toDoubleOrNull()
        if (useLivePricing) {
            viewModel.addLivePricedAsset(displayName, symbol, qty, cmcSymbol)
        } else {
            viewModel.addFixedPriceAsset(displayName, symbol, qty, fixedPrice.toDoubleOrNull())
        }
        if (validationError == null) {
            displayName = ""
            symbol = ""
            quantity = ""
            fixedPrice = ""
            cmcSymbol = ""
        }
        focusManager.clearFocus()
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            Text(
                stringResource(Res.string.customassets_title),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            )
            CptUnderlineTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(Res.string.customassets_label_field)) },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            CptUnderlineTextField(
                value = symbol,
                onValueChange = {
                    symbol = it
                    viewModel.clearValidationError()
                },
                label = { Text(stringResource(Res.string.customassets_symbol_field)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            CptUnderlineTextField(
                value = quantity,
                onValueChange = {
                    quantity = it
                    viewModel.clearValidationError()
                },
                label = { Text(stringResource(Res.string.customassets_quantity_field)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 16.dp)
                        .toggleable(
                            value = useLivePricing,
                            onValueChange = { useLivePricing = it },
                            role = Role.Switch,
                        ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.customassets_live_price_switch_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(checked = useLivePricing, onCheckedChange = null)
            }
            if (useLivePricing) {
                CptUnderlineTextField(
                    value = cmcSymbol,
                    onValueChange = {
                        cmcSymbol = it
                        viewModel.clearValidationError()
                    },
                    label = { Text(stringResource(Res.string.customassets_cmc_symbol_field)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
            } else {
                CptUnderlineTextField(
                    value = fixedPrice,
                    onValueChange = {
                        fixedPrice = it
                        viewModel.clearValidationError()
                    },
                    label = { Text(stringResource(Res.string.customassets_fixed_price_field)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
            }
            if (validationError != null) {
                Text(
                    validationError.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Button(
                onClick = submit,
                modifier = Modifier.padding(top = 20.dp),
            ) { Text(stringResource(Res.string.customassets_add_button)) }

            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                if (assets.isEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.customassets_empty_state),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(assets) { asset -> CustomAssetRow(asset, onRemove = { viewModel.removeAsset(asset.id) }) }
                }
            }
        }
    }
}

@Composable
private fun CustomAssetRow(
    asset: Account.CustomAsset,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CptCoinBadge(
                asset.assetSymbol,
                CptBadgeShape.CIRCLE,
                modifier = Modifier.padding(end = 10.dp),
            )
            Column {
                Text(asset.displayName, style = MaterialTheme.typography.bodyLarge)
                val pricingLabel =
                    when (val pricing = asset.pricing) {
                        is CustomAssetPricing.Fixed ->
                            stringResource(
                                Res.string.customassets_fixed_pricing_summary,
                                asset.quantity,
                                asset.assetSymbol,
                                pricing.unitPriceUsd,
                            )
                        is CustomAssetPricing.LiveFromCoinMarketCap ->
                            stringResource(
                                Res.string.customassets_live_pricing_summary,
                                asset.quantity,
                                asset.assetSymbol,
                                pricing.cmcSymbol,
                            )
                    }
                Text(
                    pricingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onRemove) { Text(stringResource(Res.string.customassets_remove_button)) }
    }
}
