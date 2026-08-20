package by.vsdev.cpt.core.model

data class AccountId(
    val value: String,
)

enum class ChainId {
    ETHEREUM,
    OPTIMISM,
    ARBITRUM,
    TON,
    TRON,
}

enum class ExchangeId {
    BINANCE,
    OKX,
    BYBIT,
    BITGET,
}
