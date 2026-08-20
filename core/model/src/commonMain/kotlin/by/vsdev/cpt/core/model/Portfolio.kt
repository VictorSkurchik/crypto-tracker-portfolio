package by.vsdev.cpt.core.model

import kotlin.time.Instant

data class AccountBreakdown(
    val accountId: AccountId,
    val displayName: String,
    val badge: String,
    val valueUsd: Double,
    val balances: List<PricedBalance>,
)

data class AssetBreakdown(
    val assetSymbol: String,
    val quantity: Double,
    val valueUsd: Double,
)

data class PortfolioSnapshot(
    val totalValueUsd: Double,
    val byAccount: List<AccountBreakdown>,
    val byAsset: List<AssetBreakdown>,
    val lastUpdated: Instant?,
)
