package by.vsdev.cpt.feature.wallets.fakes

import by.vsdev.cpt.core.database.dao.WalletDao
import by.vsdev.cpt.core.database.entity.WalletEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory fake for the Room DAO, so [by.vsdev.cpt.core.data.WalletsRepository] can be tested without a real database. */
class FakeWalletDao : WalletDao {
    private val state = MutableStateFlow<List<WalletEntity>>(emptyList())

    override fun observeAll(): Flow<List<WalletEntity>> = state

    override suspend fun upsert(wallet: WalletEntity) {
        state.update { it.filterNot { existing -> existing.id == wallet.id } + wallet }
    }

    override suspend fun delete(id: String) {
        state.update { it.filterNot { existing -> existing.id == id } }
    }
}
