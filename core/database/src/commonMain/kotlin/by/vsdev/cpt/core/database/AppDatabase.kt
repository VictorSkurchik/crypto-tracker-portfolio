package by.vsdev.cpt.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import by.vsdev.cpt.core.database.dao.BalanceDao
import by.vsdev.cpt.core.database.dao.CustomAssetDao
import by.vsdev.cpt.core.database.dao.ExchangeAccountDao
import by.vsdev.cpt.core.database.dao.WalletDao
import by.vsdev.cpt.core.database.entity.CachedBalanceEntity
import by.vsdev.cpt.core.database.entity.CustomAssetEntity
import by.vsdev.cpt.core.database.entity.ExchangeAccountEntity
import by.vsdev.cpt.core.database.entity.RefreshStateEntity
import by.vsdev.cpt.core.database.entity.WalletEntity

const val DATABASE_FILE_NAME = "cpt.db"

@Database(
    entities = [
        WalletEntity::class,
        ExchangeAccountEntity::class,
        CustomAssetEntity::class,
        CachedBalanceEntity::class,
        RefreshStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    abstract fun exchangeAccountDao(): ExchangeAccountDao

    abstract fun customAssetDao(): CustomAssetDao

    abstract fun balanceDao(): BalanceDao
}

// The Room KSP compiler generates the `actual` implementation of this per target.
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
