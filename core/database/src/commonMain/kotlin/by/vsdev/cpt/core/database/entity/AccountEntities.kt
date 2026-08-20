package by.vsdev.cpt.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val chain: String,
    val address: String,
)

@Entity(tableName = "exchange_accounts")
data class ExchangeAccountEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val exchange: String,
    val credentialsRef: String,
)

@Entity(tableName = "custom_assets")
data class CustomAssetEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val assetSymbol: String,
    val quantity: Double,
    val pricingType: String,
    val fixedPriceUsd: Double?,
    val cmcSymbol: String?,
)
