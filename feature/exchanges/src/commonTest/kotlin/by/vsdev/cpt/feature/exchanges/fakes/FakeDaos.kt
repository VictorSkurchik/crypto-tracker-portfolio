package by.vsdev.cpt.feature.exchanges.fakes

import by.vsdev.cpt.core.database.dao.ExchangeAccountDao
import by.vsdev.cpt.core.database.entity.ExchangeAccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// ExchangesRepository is a concrete class, so its Room DAO is faked here instead of itself.
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
