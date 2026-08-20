package by.vsdev.cpt.feature.customassets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.vsdev.cpt.core.data.CustomAssetsRepository
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.AccountId
import by.vsdev.cpt.core.model.CustomAssetPricing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomAssetsViewModel(
    private val customAssetsRepository: CustomAssetsRepository,
) : ViewModel() {
    val assets: StateFlow<List<Account.CustomAsset>> =
        customAssetsRepository
            .observeAssets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), emptyList())

    fun addFixedPriceAsset(
        displayName: String,
        symbol: String,
        quantity: Double,
        unitPriceUsd: Double,
    ) {
        if (symbol.isBlank() || quantity <= 0.0) return
        viewModelScope.launch {
            customAssetsRepository.addAsset(
                displayName.ifBlank { symbol },
                symbol.trim().uppercase(),
                quantity,
                CustomAssetPricing.Fixed(unitPriceUsd),
            )
        }
    }

    fun addLivePricedAsset(
        displayName: String,
        symbol: String,
        quantity: Double,
        cmcSymbol: String,
    ) {
        if (symbol.isBlank() || quantity <= 0.0 || cmcSymbol.isBlank()) return
        viewModelScope.launch {
            customAssetsRepository.addAsset(
                displayName.ifBlank { symbol },
                symbol.trim().uppercase(),
                quantity,
                CustomAssetPricing.LiveFromCoinMarketCap(cmcSymbol.trim().uppercase()),
            )
        }
    }

    fun removeAsset(id: AccountId) {
        viewModelScope.launch { customAssetsRepository.removeAsset(id) }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
