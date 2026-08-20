package by.vsdev.cpt.feature.wallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.vsdev.cpt.core.data.WalletsRepository
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.AccountId
import by.vsdev.cpt.core.model.ChainId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletsViewModel(
    private val walletsRepository: WalletsRepository,
) : ViewModel() {
    val wallets: StateFlow<List<Account.OnChainWallet>> =
        walletsRepository
            .observeWallets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addWallet(
        displayName: String,
        chain: ChainId,
        address: String,
    ) {
        if (address.isBlank()) return
        viewModelScope.launch {
            walletsRepository.addWallet(displayName.ifBlank { address }, chain, address.trim())
        }
    }

    fun removeWallet(id: AccountId) {
        viewModelScope.launch { walletsRepository.removeWallet(id) }
    }
}
