package by.vsdev.cpt.feature.exchanges.fakes

import by.vsdev.cpt.core.model.ExchangeConnector
import by.vsdev.cpt.core.model.ExchangeCredentials
import by.vsdev.cpt.core.model.ExchangeId
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

/** [gate], if set, suspends mid-flight so a test can observe `isVerifying == true`. */
class FakeExchangeConnector(
    override val id: ExchangeId,
    private val result: ProviderResult<List<TokenBalance>>,
    private val gate: CompletableDeferred<Unit>? = null,
) : ExchangeConnector {
    override suspend fun fetchBalances(credentials: ExchangeCredentials): ProviderResult<List<TokenBalance>> {
        gate?.await()
        return result
    }
}
