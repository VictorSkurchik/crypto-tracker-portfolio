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
            // The secret was written but the row it's referenced by never made it into the DB —
            // remove it so it doesn't linger as an orphaned, unreferenced entry, then surface the
            // original failure.
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
     * Removes the secret before the DB row (the reverse of [addAccount]'s write order) so that if
     * one half fails, the account is left in a self-healing state instead of an orphaned one: a
     * failure deleting the secret leaves the row (and its secret) untouched for a clean retry; a
     * failure deleting the row afterwards leaves it referencing an already-gone secret, which
     * [resolveCredentials] already treats as a plain "missing credentials" account-level error —
     * and a retried [removeAccount] call still finishes the job, since removing an already-removed
     * secret is a no-op.
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
 * Returns `null` (instead of throwing, like [ExchangeId.valueOf] would) when
 * [ExchangeAccountEntity.exchange] doesn't match any current [ExchangeId] constant, so an
 * exchange removed/renamed in a future release just drops that one stored account from the list
 * rather than crashing [observeAccounts] — and with it the whole Exchanges/Portfolio screen — for
 * every user with that value persisted.
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
