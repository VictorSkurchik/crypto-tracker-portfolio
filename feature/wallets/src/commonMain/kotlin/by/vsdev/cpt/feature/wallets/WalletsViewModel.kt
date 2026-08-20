package by.vsdev.cpt.feature.wallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.vsdev.cpt.core.data.WalletsRepository
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.AccountId
import by.vsdev.cpt.core.model.ChainId
import by.vsdev.cpt.core.model.WalletAddressValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletsViewModel(
    private val walletsRepository: WalletsRepository,
) : ViewModel() {
    val wallets: StateFlow<List<Account.OnChainWallet>> =
        walletsRepository
            .observeWallets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), emptyList())

    private val _addressError = MutableStateFlow<String?>(null)
    val addressError: StateFlow<String?> = _addressError.asStateFlow()

    fun addWallet(
        displayName: String,
        chain: ChainId,
        address: String,
    ) {
        val trimmedAddress = address.trim()
        if (!WalletAddressValidator.isValid(chain, trimmedAddress)) {
            _addressError.value = "That doesn't look like a valid ${chain.name} address"
            return
        }
        _addressError.value = null
        viewModelScope.launch {
            walletsRepository.addWallet(displayName.ifBlank { trimmedAddress }, chain, trimmedAddress)
        }
    }

    fun clearAddressError() {
        _addressError.value = null
    }

    fun removeWallet(id: AccountId) {
        viewModelScope.launch { walletsRepository.removeWallet(id) }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
