package by.vsdev.cpt.feature.portfolio.fakes

import by.vsdev.cpt.core.database.dao.BalanceDao
import by.vsdev.cpt.core.database.dao.CustomAssetDao
import by.vsdev.cpt.core.database.dao.ExchangeAccountDao
import by.vsdev.cpt.core.database.dao.WalletDao
import by.vsdev.cpt.core.database.entity.CachedBalanceEntity
import by.vsdev.cpt.core.database.entity.CustomAssetEntity
import by.vsdev.cpt.core.database.entity.ExchangeAccountEntity
import by.vsdev.cpt.core.database.entity.RefreshStateEntity
import by.vsdev.cpt.core.database.entity.WalletEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// PortfolioRepository is a concrete class, so its Room DAOs are faked here instead of itself.
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

class FakeExchangeAccountDao : ExchangeAccountDao {
    private val state = MutableStateFlow<List<ExchangeAccountEntity>>(emptyList())

    override fun observeAll(): Flow<List<ExchangeAccountEntity>> = state

    override suspend fun upsert(account: ExchangeAccountEntity) {
        state.update { it.filterNot { existing -> existing.id == account.id } + account }
    }

    override suspend fun delete(id: String) {
        state.update { it.filterNot { existing -> existing.id == id } }
    }
}

class FakeCustomAssetDao : CustomAssetDao {
    private val state = MutableStateFlow<List<CustomAssetEntity>>(emptyList())

    override fun observeAll(): Flow<List<CustomAssetEntity>> = state

    override suspend fun upsert(asset: CustomAssetEntity) {
        state.update { it.filterNot { existing -> existing.id == asset.id } + asset }
    }

    override suspend fun delete(id: String) {
        state.update { it.filterNot { existing -> existing.id == id } }
    }
}

class FakeBalanceDao : BalanceDao {
    private val balances = MutableStateFlow<List<CachedBalanceEntity>>(emptyList())
    private val refreshState = MutableStateFlow<RefreshStateEntity?>(null)

    override fun observeAll(): Flow<List<CachedBalanceEntity>> = balances

    override suspend fun clearForAccount(accountId: String) {
        balances.update { it.filterNot { existing -> existing.accountId == accountId } }
    }

    override suspend fun upsertAll(balances: List<CachedBalanceEntity>) {
        this.balances.update { existing ->
            val keys = balances.map { it.accountId to it.assetSymbol }.toSet()
            existing.filterNot { (it.accountId to it.assetSymbol) in keys } + balances
        }
    }

    override fun observeRefreshState(): Flow<RefreshStateEntity?> = refreshState

    override suspend fun upsertRefreshState(state: RefreshStateEntity) {
        refreshState.value = state
    }
}
