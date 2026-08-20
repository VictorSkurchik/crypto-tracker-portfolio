package by.vsdev.cpt.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The latest known balance for one asset within one account — overwritten wholesale on every refresh. */
@Entity(tableName = "cached_balances", primaryKeys = ["accountId", "assetSymbol"])
data class CachedBalanceEntity(
    val accountId: String,
    val assetSymbol: String,
    val quantity: Double,
    val priceUsd: Double,
    val valueUsd: Double,
    val chain: String?,
    val lastUpdatedEpochMillis: Long,
)

/** Singleton row (fixed id = 0) tracking when the whole portfolio was last refreshed. */
@Entity(tableName = "refresh_state")
data class RefreshStateEntity(
    @PrimaryKey val id: Int = 0,
    val lastRefreshedEpochMillis: Long?,
)
