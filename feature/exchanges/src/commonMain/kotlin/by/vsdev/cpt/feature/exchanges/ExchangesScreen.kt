package by.vsdev.cpt.feature.exchanges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.vsdev.cpt.core.designsystem.CptBadgeShape
import by.vsdev.cpt.core.designsystem.CptChip
import by.vsdev.cpt.core.designsystem.CptCoinBadge
import by.vsdev.cpt.core.designsystem.CptPasswordField
import by.vsdev.cpt.core.designsystem.CptUnderlineTextField
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.ExchangeId
import crypto_portfolio_tracker.feature.exchanges.generated.resources.Res
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_add_title
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_api_key_label
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_api_secret_label
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_connect
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_connection_failed
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_empty_state
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_exchange_label
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_label_optional
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_passphrase_label
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_readonly_hint
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_remove
import crypto_portfolio_tracker.feature.exchanges.generated.resources.exchanges_verifying
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private fun ExchangeId.iconSymbol(): String =
    when (this) {
        ExchangeId.BINANCE -> "BN"
        ExchangeId.OKX -> "OK"
        ExchangeId.BYBIT -> "BY"
        ExchangeId.BITGET -> "BG"
    }

/** [ExchangeId] has no `Parcelable`/direct `Saver` support in commonMain, so persist it across
 * process death as its [ExchangeId.name] string instead. */
private val ExchangeIdSaver =
    Saver<ExchangeId, String>(
        save = { it.name },
        restore = { name -> ExchangeId.entries.firstOrNull { it.name == name } ?: ExchangeId.BINANCE },
    )

@Composable
fun ExchangesScreen(viewModel: ExchangesViewModel = koinViewModel()) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val connectState by viewModel.connectState.collectAsStateWithLifecycle()
    var displayName by rememberSaveable { mutableStateOf("") }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var apiSecret by rememberSaveable { mutableStateOf("") }
    var passphrase by rememberSaveable { mutableStateOf("") }
    var exchange by rememberSaveable(stateSaver = ExchangeIdSaver) { mutableStateOf(ExchangeId.BINANCE) }

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

    val focusManager = LocalFocusManager.current
    val onConnect = { viewModel.addAccount(displayName, exchange, apiKey, apiSecret, passphrase) }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            Text(stringResource(Res.string.exchanges_add_title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp))
            Text(
                stringResource(Res.string.exchanges_readonly_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            )
            Text(
                stringResource(Res.string.exchanges_exchange_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                label = { Text(stringResource(Res.string.exchanges_label_optional)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            CptUnderlineTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    viewModel.clearConnectionError()
                },
                label = { Text(stringResource(Res.string.exchanges_api_key_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            CptPasswordField(
                value = apiSecret,
                onValueChange = {
                    apiSecret = it
                    viewModel.clearConnectionError()
                },
                label = { Text(stringResource(Res.string.exchanges_api_secret_label)) },
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            if (exchange.requiresPassphrase()) focusManager.moveFocus(FocusDirection.Down) else onConnect()
                        },
                    ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (exchange.requiresPassphrase()) {
                CptPasswordField(
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        viewModel.clearConnectionError()
                    },
                    label = { Text(stringResource(Res.string.exchanges_passphrase_label)) },
                    keyboardActions = KeyboardActions(onDone = { onConnect() }),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            if (connectState.connectionError != null) {
                Text(
                    stringResource(Res.string.exchanges_connection_failed, connectState.connectionError.orEmpty()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Button(
                onClick = onConnect,
                enabled = !connectState.isVerifying,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                if (connectState.isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                }
                Text(stringResource(if (connectState.isVerifying) Res.string.exchanges_verifying else Res.string.exchanges_connect))
            }

            if (accounts.isEmpty()) {
                Text(
                    stringResource(Res.string.exchanges_empty_state),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                    items(accounts) { account ->
                        ExchangeAccountRow(account, onRemove = { viewModel.removeAccount(account.id, account.credentialsRef) })
                    }
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
        TextButton(onClick = onRemove) { Text(stringResource(Res.string.exchanges_remove)) }
    }
}
