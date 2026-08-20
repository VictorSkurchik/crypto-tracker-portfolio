package by.vsdev.cpt.feature.customassets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.vsdev.cpt.core.data.CustomAssetsRepository
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.AccountId
import by.vsdev.cpt.core.model.CustomAssetPricing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomAssetsViewModel(
    private val customAssetsRepository: CustomAssetsRepository,
) : ViewModel() {
    val assets: StateFlow<List<Account.CustomAsset>> =
        customAssetsRepository
            .observeAssets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), emptyList())

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    fun addFixedPriceAsset(
        displayName: String,
        symbol: String,
        quantity: Double?,
        unitPriceUsd: Double?,
    ) {
        val error =
            when {
                symbol.isBlank() -> "Symbol is required"
                quantity == null -> "Quantity must be a valid number"
                quantity <= 0.0 -> "Quantity must be greater than zero"
                unitPriceUsd == null -> "Price must be a valid number"
                unitPriceUsd <= 0.0 -> "Price must be greater than zero"
                else -> null
            }
        if (error != null) {
            _validationError.value = error
            return
        }
        _validationError.value = null
        viewModelScope.launch {
            customAssetsRepository.addAsset(
                displayName.ifBlank { symbol },
                symbol.trim().uppercase(),
                checkNotNull(quantity),
                CustomAssetPricing.Fixed(checkNotNull(unitPriceUsd)),
            )
        }
    }

    fun addLivePricedAsset(
        displayName: String,
        symbol: String,
        quantity: Double?,
        cmcSymbol: String,
    ) {
        val error =
            when {
                symbol.isBlank() -> "Symbol is required"
                quantity == null -> "Quantity must be a valid number"
                quantity <= 0.0 -> "Quantity must be greater than zero"
                cmcSymbol.isBlank() -> "CoinMarketCap symbol is required"
                else -> null
            }
        if (error != null) {
            _validationError.value = error
            return
        }
        _validationError.value = null
        viewModelScope.launch {
            customAssetsRepository.addAsset(
                displayName.ifBlank { symbol },
                symbol.trim().uppercase(),
                checkNotNull(quantity),
                CustomAssetPricing.LiveFromCoinMarketCap(cmcSymbol.trim().uppercase()),
            )
        }
    }

    fun clearValidationError() {
        _validationError.value = null
    }

    fun removeAsset(id: AccountId) {
        viewModelScope.launch { customAssetsRepository.removeAsset(id) }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
