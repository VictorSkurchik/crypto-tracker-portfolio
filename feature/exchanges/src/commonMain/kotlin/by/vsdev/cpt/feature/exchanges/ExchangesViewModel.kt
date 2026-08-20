package by.vsdev.cpt.feature.exchanges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.vsdev.cpt.core.data.ExchangesRepository
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.AccountId
import by.vsdev.cpt.core.model.ExchangeConnectorRegistry
import by.vsdev.cpt.core.model.ExchangeCredentials
import by.vsdev.cpt.core.model.ExchangeId
import by.vsdev.cpt.core.model.ProviderResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** OKX and Bitget need a passphrase in addition to the key/secret; Binance and Bybit don't. */
fun ExchangeId.requiresPassphrase(): Boolean = this == ExchangeId.OKX || this == ExchangeId.BITGET

data class ConnectAccountUiState(
    val isVerifying: Boolean = false,
    val connectionError: String? = null,
)

class ExchangesViewModel(
    private val exchangesRepository: ExchangesRepository,
    private val exchangeConnectorRegistry: ExchangeConnectorRegistry,
) : ViewModel() {
    val accounts: StateFlow<List<Account.ExchangeAccount>> =
        exchangesRepository
            .observeAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), emptyList())

    private val _connectState = MutableStateFlow(ConnectAccountUiState())
    val connectState: StateFlow<ConnectAccountUiState> = _connectState.asStateFlow()

    /** Verifies the credentials with a real API call before persisting them — a wrong key/secret
     * should fail loudly here, not silently on the next portfolio refresh. */
    fun addAccount(
        displayName: String,
        exchange: ExchangeId,
        apiKey: String,
        apiSecret: String,
        passphrase: String,
    ) {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            _connectState.value = ConnectAccountUiState(connectionError = "API key and secret are required")
            return
        }
        val credentials =
            if (exchange.requiresPassphrase()) {
                ExchangeCredentials.ApiKeySecretPassphrase(apiKey.trim(), apiSecret.trim(), passphrase.trim())
            } else {
                ExchangeCredentials.ApiKeySecret(apiKey.trim(), apiSecret.trim())
            }
        val connector = exchangeConnectorRegistry.resolve(exchange)
        if (connector == null) {
            _connectState.value = ConnectAccountUiState(connectionError = "No connector registered for $exchange")
            return
        }
        viewModelScope.launch {
            _connectState.value = ConnectAccountUiState(isVerifying = true)
            when (val result = connector.fetchBalances(credentials)) {
                is ProviderResult.Success -> {
                    exchangesRepository.addAccount(displayName.ifBlank { exchange.name }, exchange, credentials)
                    _connectState.value = ConnectAccountUiState()
                }
                is ProviderResult.Failure ->
                    _connectState.value = ConnectAccountUiState(connectionError = result.error.message)
            }
        }
    }

    fun clearConnectionError() {
        _connectState.value = _connectState.value.copy(connectionError = null)
    }

    fun removeAccount(
        id: AccountId,
        credentialsRef: String,
    ) {
        viewModelScope.launch { exchangesRepository.removeAccount(id, credentialsRef) }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
