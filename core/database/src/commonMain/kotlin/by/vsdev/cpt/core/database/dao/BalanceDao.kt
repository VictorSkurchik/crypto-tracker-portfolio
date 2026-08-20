package by.vsdev.cpt.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import by.vsdev.cpt.core.database.entity.CachedBalanceEntity
import by.vsdev.cpt.core.database.entity.RefreshStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceDao {
    @Query("SELECT * FROM cached_balances")
    fun observeAll(): Flow<List<CachedBalanceEntity>>

    @Query("DELETE FROM cached_balances WHERE accountId = :accountId")
    suspend fun clearForAccount(accountId: String)

    @Upsert
    suspend fun upsertAll(balances: List<CachedBalanceEntity>)

    @Query("SELECT * FROM refresh_state WHERE id = 0")
    fun observeRefreshState(): Flow<RefreshStateEntity?>

    @Upsert
    suspend fun upsertRefreshState(state: RefreshStateEntity)
}
