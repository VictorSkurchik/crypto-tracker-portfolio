package by.vsdev.cpt.core.data.fakes

import by.vsdev.cpt.core.model.ChainId
import by.vsdev.cpt.core.model.ExchangeConnector
import by.vsdev.cpt.core.model.ExchangeConnectorRegistry
import by.vsdev.cpt.core.model.ExchangeCredentials
import by.vsdev.cpt.core.model.ExchangeId
import by.vsdev.cpt.core.model.OnChainProvider
import by.vsdev.cpt.core.model.OnChainProviderRegistry
import by.vsdev.cpt.core.model.PriceProvider
import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import by.vsdev.cpt.core.model.TokenBalance
import by.vsdev.cpt.core.secrets.SecretStore

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

/** Always returns [balances] for a matching address, or [error] if one was configured for that address. */
class FakeOnChainProvider(
    private val chain: ChainId,
    private val balancesByAddress: Map<String, List<TokenBalance>> = emptyMap(),
    private val errorsByAddress: Map<String, ProviderError> = emptyMap(),
) : OnChainProvider {
    override fun supports(chainId: ChainId): Boolean = chainId == chain

    override suspend fun fetchBalances(
        chainId: ChainId,
        address: String,
    ): ProviderResult<List<TokenBalance>> {
        errorsByAddress[address]?.let { return ProviderResult.Failure(it) }
        return ProviderResult.Success(balancesByAddress[address].orEmpty())
    }
}

class FakeExchangeConnector(
    override val id: ExchangeId,
    private val result: ProviderResult<List<TokenBalance>>,
) : ExchangeConnector {
    override suspend fun fetchBalances(credentials: ExchangeCredentials): ProviderResult<List<TokenBalance>> = result
}

class FakeOnChainProviderRegistry(
    private val providers: List<OnChainProvider>,
) : OnChainProviderRegistry {
    override fun resolve(chainId: ChainId): OnChainProvider? = providers.firstOrNull { it.supports(chainId) }
}

class FakeExchangeConnectorRegistry(
    connectors: List<ExchangeConnector>,
) : ExchangeConnectorRegistry {
    private val byId = connectors.associateBy { it.id }

    override fun resolve(exchangeId: ExchangeId): ExchangeConnector? = byId[exchangeId]
}

class FakePriceProvider(
    private val prices: Map<String, Double> = emptyMap(),
) : PriceProvider {
    override suspend fun getPrices(symbols: Set<String>): ProviderResult<Map<String, Double>> =
        ProviderResult.Success(symbols.mapNotNull { symbol -> prices[symbol]?.let { symbol to it } }.toMap())
}
