package by.vsdev.cpt.core.data

import by.vsdev.cpt.core.database.dao.ExchangeAccountDao
import by.vsdev.cpt.core.database.entity.ExchangeAccountEntity
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.AccountId
import by.vsdev.cpt.core.model.ExchangeCredentials
import by.vsdev.cpt.core.model.ExchangeId
import by.vsdev.cpt.core.secrets.SecretStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * [Account.ExchangeAccount] never carries credentials directly (see :core:model docs) — this
 * repository is the only place that resolves a `credentialsRef` to the real [ExchangeCredentials],
 * and it only ever does so transiently, right before a sync call.
 */
class ExchangesRepository(
    private val exchangeAccountDao: ExchangeAccountDao,
    private val secretStore: SecretStore,
) {
    fun observeAccounts(): Flow<List<Account.ExchangeAccount>> =
        exchangeAccountDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addAccount(
        displayName: String,
        exchange: ExchangeId,
        credentials: ExchangeCredentials,
    ): Account.ExchangeAccount {
        val id = AccountId(Uuid.random().toString())
        val credentialsRef = "exchange_credentials_${id.value}"
        secretStore.store(credentialsRef, Json.encodeToString(SerializableCredentials.serializer(), credentials.toSerializable()))
        exchangeAccountDao.upsert(
            ExchangeAccountEntity(id = id.value, displayName = displayName, exchange = exchange.name, credentialsRef = credentialsRef),
        )
        return Account.ExchangeAccount(id = id, displayName = displayName, exchange = exchange, credentialsRef = credentialsRef)
    }

    suspend fun resolveCredentials(credentialsRef: String): ExchangeCredentials? {
        val json = secretStore.retrieve(credentialsRef) ?: return null
        return Json.decodeFromString(SerializableCredentials.serializer(), json).toDomain()
    }

    suspend fun removeAccount(
        id: AccountId,
        credentialsRef: String,
    ) {
        exchangeAccountDao.delete(id.value)
        secretStore.remove(credentialsRef)
    }
}

private fun ExchangeAccountEntity.toDomain() =
    Account.ExchangeAccount(
        id = AccountId(id),
        displayName = displayName,
        exchange = ExchangeId.valueOf(exchange),
        credentialsRef = credentialsRef,
    )

@Serializable
private data class SerializableCredentials(
    val apiKey: String,
    val apiSecret: String,
    val passphrase: String? = null,
)

private fun ExchangeCredentials.toSerializable(): SerializableCredentials =
    when (this) {
        is ExchangeCredentials.ApiKeySecret -> SerializableCredentials(apiKey, apiSecret)
        is ExchangeCredentials.ApiKeySecretPassphrase -> SerializableCredentials(apiKey, apiSecret, passphrase)
    }

private fun SerializableCredentials.toDomain(): ExchangeCredentials =
    if (passphrase != null) {
        ExchangeCredentials.ApiKeySecretPassphrase(apiKey, apiSecret, passphrase)
    } else {
        ExchangeCredentials.ApiKeySecret(apiKey, apiSecret)
    }
