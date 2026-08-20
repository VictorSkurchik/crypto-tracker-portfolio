package by.vsdev.cpt.feature.exchanges.fakes

import by.vsdev.cpt.core.database.dao.ExchangeAccountDao
import by.vsdev.cpt.core.database.entity.ExchangeAccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory fake for the Room DAO `ExchangesRepository` depends on, mirroring the pattern
 * `:feature:portfolio` and `:core:data` already use one level down — `ExchangesRepository` is a
 * concrete class, so testing the ViewModel means faking its dependency rather than itself.
 */
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
