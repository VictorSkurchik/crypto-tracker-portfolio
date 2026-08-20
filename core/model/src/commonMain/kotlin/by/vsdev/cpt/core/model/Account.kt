package by.vsdev.cpt.core.model

/**
 * The three kinds of thing a user can add to their portfolio. [ExchangeAccount] never carries
 * credentials directly — only an opaque [credentialsRef] key into a platform-specific secret
 * store (see :core:secrets), so this type is structurally incapable of leaking a secret via
 * logging, caching, or accidental serialization.
 */
sealed class Account {
    abstract val id: AccountId
    abstract val displayName: String

    data class OnChainWallet(
        override val id: AccountId,
        override val displayName: String,
        val chain: ChainId,
        val address: String,
    ) : Account()

    data class ExchangeAccount(
        override val id: AccountId,
        override val displayName: String,
        val exchange: ExchangeId,
        val credentialsRef: String,
    ) : Account()

    data class CustomAsset(
        override val id: AccountId,
        override val displayName: String,
        val assetSymbol: String,
        val quantity: Double,
        val pricing: CustomAssetPricing,
    ) : Account()
}

sealed interface CustomAssetPricing {
    data class Fixed(
        val unitPriceUsd: Double,
    ) : CustomAssetPricing

    data class LiveFromCoinMarketCap(
        val cmcSymbol: String,
    ) : CustomAssetPricing
}
