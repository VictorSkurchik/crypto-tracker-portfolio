package by.vsdev.cpt.feature.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.vsdev.cpt.core.data.PortfolioRepository
import by.vsdev.cpt.core.model.PortfolioSnapshot
import by.vsdev.cpt.core.model.ProviderError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PortfolioUiState(
    val snapshot: PortfolioSnapshot? = null,
    val isRefreshing: Boolean = false,
    val lastErrors: Map<String, ProviderError?> = emptyMap(),
)

class PortfolioViewModel(
    private val portfolioRepository: PortfolioRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            portfolioRepository.observeSnapshot().collect { snapshot ->
                _uiState.value = _uiState.value.copy(snapshot = snapshot)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val errors = portfolioRepository.refresh()
            _uiState.value = _uiState.value.copy(isRefreshing = false, lastErrors = errors)
        }
    }
}
