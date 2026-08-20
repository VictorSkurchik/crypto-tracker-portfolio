package by.vsdev.cpt.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import by.vsdev.cpt.core.database.entity.CustomAssetEntity
import by.vsdev.cpt.core.database.entity.ExchangeAccountEntity
import by.vsdev.cpt.core.database.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY displayName")
    fun observeAll(): Flow<List<WalletEntity>>

    @Upsert
    suspend fun upsert(wallet: WalletEntity)

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ExchangeAccountDao {
    @Query("SELECT * FROM exchange_accounts ORDER BY displayName")
    fun observeAll(): Flow<List<ExchangeAccountEntity>>

    @Upsert
    suspend fun upsert(account: ExchangeAccountEntity)

    @Query("DELETE FROM exchange_accounts WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface CustomAssetDao {
    @Query("SELECT * FROM custom_assets ORDER BY displayName")
    fun observeAll(): Flow<List<CustomAssetEntity>>

    @Upsert
    suspend fun upsert(asset: CustomAssetEntity)

    @Query("DELETE FROM custom_assets WHERE id = :id")
    suspend fun delete(id: String)
}
