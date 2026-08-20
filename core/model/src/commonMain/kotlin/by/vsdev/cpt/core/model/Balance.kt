package by.vsdev.cpt.core.model

/** A raw holding as reported by a provider — no USD value yet. */
data class TokenBalance(
    val assetSymbol: String,
    val quantity: Double,
    val chain: ChainId? = null,
)

/** A [TokenBalance] after a [PriceProvider] has priced it. */
data class PricedBalance(
    val assetSymbol: String,
    val quantity: Double,
    val priceUsd: Double,
    val valueUsd: Double,
)
