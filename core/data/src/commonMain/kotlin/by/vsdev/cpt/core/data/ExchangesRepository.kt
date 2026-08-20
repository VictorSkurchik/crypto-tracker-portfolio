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

/** The only place that resolves a `credentialsRef` to real [ExchangeCredentials], transiently, right before a sync call. */
class ExchangesRepository(
    private val exchangeAccountDao: ExchangeAccountDao,
    private val secretStore: SecretStore,
) {
    fun observeAccounts(): Flow<List<Account.ExchangeAccount>> =
        exchangeAccountDao.observeAll().map { entities ->
            entities.mapNotNull { it.toDomain() }
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
        try {
            exchangeAccountDao.upsert(
                ExchangeAccountEntity(id = id.value, displayName = displayName, exchange = exchange.name, credentialsRef = credentialsRef),
            )
        } catch (e: Exception) {
            // DB write failed after the secret was already stored — remove it to avoid an orphan.
            secretStore.remove(credentialsRef)
            throw e
        }
        return Account.ExchangeAccount(id = id, displayName = displayName, exchange = exchange, credentialsRef = credentialsRef)
    }

    suspend fun resolveCredentials(credentialsRef: String): ExchangeCredentials? {
        val json = secretStore.retrieve(credentialsRef) ?: return null
        return Json.decodeFromString(SerializableCredentials.serializer(), json).toDomain()
    }

    /**
     * Removes the secret before the DB row (reverse of [addAccount]'s order): if this fails
     * partway through, the account is left in a self-healing state — either untouched for a clean
     * retry, or referencing an already-gone secret, which [resolveCredentials] already treats as a
     * plain "missing credentials" error — rather than an orphaned secret.
     */
    suspend fun removeAccount(
        id: AccountId,
        credentialsRef: String,
    ) {
        secretStore.remove(credentialsRef)
        exchangeAccountDao.delete(id.value)
    }
}

/**
 * Unlike [ExchangeId.valueOf], returns `null` instead of throwing on an unrecognized
 * [ExchangeAccountEntity.exchange], so a renamed/removed enum constant drops one stored row
 * instead of crashing [observeAccounts].
 */
private fun ExchangeAccountEntity.toDomain(): Account.ExchangeAccount? {
    val exchangeId = ExchangeId.entries.firstOrNull { it.name == exchange } ?: return null
    return Account.ExchangeAccount(
        id = AccountId(id),
        displayName = displayName,
        exchange = exchangeId,
        credentialsRef = credentialsRef,
    )
}

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
