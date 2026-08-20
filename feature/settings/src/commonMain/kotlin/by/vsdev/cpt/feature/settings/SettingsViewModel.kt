package by.vsdev.cpt.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.vsdev.cpt.core.datastore.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val coinMarketCapApiKey: String = "",
    val etherscanApiKey: String = "",
)

class SettingsViewModel(
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value =
                SettingsUiState(
                    coinMarketCapApiKey = appPreferences.coinMarketCapApiKey().orEmpty(),
                    etherscanApiKey = appPreferences.etherscanApiKey().orEmpty(),
                )
        }
    }

    fun setCoinMarketCapApiKey(value: String) {
        _uiState.value = _uiState.value.copy(coinMarketCapApiKey = value)
        viewModelScope.launch { appPreferences.setCoinMarketCapApiKey(value) }
    }

    fun setEtherscanApiKey(value: String) {
        _uiState.value = _uiState.value.copy(etherscanApiKey = value)
        viewModelScope.launch { appPreferences.setEtherscanApiKey(value) }
    }
}
