package by.vsdev.cpt.core.data

import by.vsdev.cpt.core.database.dao.WalletDao
import by.vsdev.cpt.core.database.entity.WalletEntity
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.AccountId
import by.vsdev.cpt.core.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class WalletsRepository(
    private val walletDao: WalletDao,
) {
    fun observeWallets(): Flow<List<Account.OnChainWallet>> =
        walletDao.observeAll().map { entities ->
            entities.mapNotNull { it.toDomain() }
        }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addWallet(
        displayName: String,
        chain: ChainId,
        address: String,
    ): Account.OnChainWallet {
        val id = AccountId(Uuid.random().toString())
        walletDao.upsert(
            WalletEntity(id = id.value, displayName = displayName, chain = chain.name, address = address),
        )
        return Account.OnChainWallet(id = id, displayName = displayName, chain = chain, address = address)
    }

    suspend fun removeWallet(id: AccountId) {
        walletDao.delete(id.value)
    }
}

/**
 * Unlike [ChainId.valueOf], returns `null` instead of throwing on an unrecognized
 * [WalletEntity.chain], so a renamed/removed enum constant drops one stored row instead of
 * crashing [observeWallets].
 */
private fun WalletEntity.toDomain(): Account.OnChainWallet? {
    val chainId = ChainId.entries.firstOrNull { it.name == chain } ?: return null
    return Account.OnChainWallet(
        id = AccountId(id),
        displayName = displayName,
        chain = chainId,
        address = address,
    )
}
