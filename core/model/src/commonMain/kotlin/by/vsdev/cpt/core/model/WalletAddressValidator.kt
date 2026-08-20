package by.vsdev.cpt.core.model

/**
 * Lightweight per-chain format sanity checks — catches obvious typos/garbage before an address
 * ever reaches a provider, rather than only surfacing as an opaque sync failure on refresh.
 * Not full checksum/base58 validation, which isn't needed for MVP scope.
 */
object WalletAddressValidator {
    fun isValid(
        chainId: ChainId,
        address: String,
    ): Boolean =
        when (chainId) {
            ChainId.ETHEREUM, ChainId.OPTIMISM, ChainId.ARBITRUM -> EVM_ADDRESS.matches(address)
            ChainId.TON -> TON_ADDRESS.matches(address)
            ChainId.TRON -> TRON_ADDRESS.matches(address)
        }

    private val EVM_ADDRESS = Regex("^0x[a-fA-F0-9]{40}$")
    private val TON_ADDRESS = Regex("^[A-Za-z0-9_-]{48}$")
    private val TRON_ADDRESS = Regex("^T[1-9A-HJ-NP-Za-km-z]{33}$")
}
