package by.vsdev.cpt.feature.exchanges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.vsdev.cpt.core.data.ExchangesRepository
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.AccountId
import by.vsdev.cpt.core.model.ExchangeCredentials
import by.vsdev.cpt.core.model.ExchangeId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** OKX and Bitget need a passphrase in addition to the key/secret; Binance and Bybit don't. */
fun ExchangeId.requiresPassphrase(): Boolean = this == ExchangeId.OKX || this == ExchangeId.BITGET

class ExchangesViewModel(
    private val exchangesRepository: ExchangesRepository,
) : ViewModel() {
    val accounts: StateFlow<List<Account.ExchangeAccount>> =
        exchangesRepository
            .observeAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), emptyList())

    fun addAccount(
        displayName: String,
        exchange: ExchangeId,
        apiKey: String,
        apiSecret: String,
        passphrase: String,
    ) {
        if (apiKey.isBlank() || apiSecret.isBlank()) return
        val credentials =
            if (exchange.requiresPassphrase()) {
                ExchangeCredentials.ApiKeySecretPassphrase(apiKey.trim(), apiSecret.trim(), passphrase.trim())
            } else {
                ExchangeCredentials.ApiKeySecret(apiKey.trim(), apiSecret.trim())
            }
        viewModelScope.launch {
            exchangesRepository.addAccount(displayName.ifBlank { exchange.name }, exchange, credentials)
        }
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
