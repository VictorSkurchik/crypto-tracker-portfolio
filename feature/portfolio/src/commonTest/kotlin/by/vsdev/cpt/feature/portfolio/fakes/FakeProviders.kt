package by.vsdev.cpt.feature.portfolio.fakes

import by.vsdev.cpt.core.model.ExchangeConnector
import by.vsdev.cpt.core.model.ExchangeCredentials
import by.vsdev.cpt.core.model.ExchangeId
import by.vsdev.cpt.core.model.PriceProvider
import by.vsdev.cpt.core.model.ProviderResult
import by.vsdev.cpt.core.model.TokenBalance
import by.vsdev.cpt.core.secrets.SecretStore
import kotlinx.coroutines.CompletableDeferred

class FakeSecretStore : SecretStore {
    private val values = mutableMapOf<String, String>()

    override suspend fun store(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override suspend fun retrieve(key: String): String? = values[key]

    override suspend fun remove(key: String) {
        values.remove(key)
    }
}

class FakeExchangeConnector(
    override val id: ExchangeId,
    private val result: ProviderResult<List<TokenBalance>>,
) : ExchangeConnector {
    override suspend fun fetchBalances(credentials: ExchangeCredentials): ProviderResult<List<TokenBalance>> = result
}

/**
 * Tracks how many times [getPrices] was called, so tests can assert a refresh happened exactly
 * once. An optional [gate] lets a test suspend the call mid-flight (e.g. to observe
 * `isRefreshing == true` before letting the refresh complete).
 */
class FakePriceProvider(
    private val prices: Map<String, Double> = emptyMap(),
    private val gate: CompletableDeferred<Unit>? = null,
) : PriceProvider {
    var callCount: Int = 0
        private set

    override suspend fun getPrices(symbols: Set<String>): ProviderResult<Map<String, Double>> {
        callCount++
        gate?.await()
        return ProviderResult.Success(symbols.mapNotNull { symbol -> prices[symbol]?.let { symbol to it } }.toMap())
    }
}
