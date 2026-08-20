package by.vsdev.cpt.core.model

sealed interface ExchangeCredentials {
    data class ApiKeySecret(
        val apiKey: String,
        val apiSecret: String,
    ) : ExchangeCredentials

    data class ApiKeySecretPassphrase(
        val apiKey: String,
        val apiSecret: String,
        val passphrase: String,
    ) : ExchangeCredentials
}

/** One implementation per on-chain network family (EVM chains share one implementation; TON/TRON each need their own). */
interface OnChainProvider {
    fun supports(chainId: ChainId): Boolean

    suspend fun fetchBalances(
        chainId: ChainId,
        address: String,
    ): ProviderResult<List<TokenBalance>>
}

/** One implementation per exchange. */
interface ExchangeConnector {
    val id: ExchangeId

    suspend fun fetchBalances(credentials: ExchangeCredentials): ProviderResult<List<TokenBalance>>
}

interface PriceProvider {
    suspend fun getPrices(symbols: Set<String>): ProviderResult<Map<String, Double>>
}

interface OnChainProviderRegistry {
    fun resolve(chainId: ChainId): OnChainProvider?
}

class DefaultOnChainProviderRegistry(
    private val providers: List<OnChainProvider>,
) : OnChainProviderRegistry {
    override fun resolve(chainId: ChainId): OnChainProvider? = providers.firstOrNull { it.supports(chainId) }
}

interface ExchangeConnectorRegistry {
    fun resolve(exchangeId: ExchangeId): ExchangeConnector?
}

class DefaultExchangeConnectorRegistry(
    connectors: List<ExchangeConnector>,
) : ExchangeConnectorRegistry {
    private val byId = connectors.associateBy { it.id }

    override fun resolve(exchangeId: ExchangeId): ExchangeConnector? = byId[exchangeId]
}
