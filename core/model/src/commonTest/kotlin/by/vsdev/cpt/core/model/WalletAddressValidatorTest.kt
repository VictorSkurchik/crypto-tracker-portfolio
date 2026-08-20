package by.vsdev.cpt.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletAddressValidatorTest {
    @Test
    fun `accepts a well-formed EVM address on Ethereum Optimism and Arbitrum`() {
        val address = "0x1234567890abcdef1234567890ABCDEF12345678"
        assertTrue(WalletAddressValidator.isValid(ChainId.ETHEREUM, address))
        assertTrue(WalletAddressValidator.isValid(ChainId.OPTIMISM, address))
        assertTrue(WalletAddressValidator.isValid(ChainId.ARBITRUM, address))
    }

    @Test
    fun `rejects an EVM address missing the 0x prefix or with the wrong length`() {
        assertFalse(WalletAddressValidator.isValid(ChainId.ETHEREUM, "1234567890abcdef1234567890abcdef12345678"))
        assertFalse(WalletAddressValidator.isValid(ChainId.ETHEREUM, "0x1234"))
        assertFalse(WalletAddressValidator.isValid(ChainId.ETHEREUM, ""))
    }

    @Test
    fun `accepts a 48-character TON user-friendly address`() {
        assertTrue(WalletAddressValidator.isValid(ChainId.TON, "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"))
    }

    @Test
    fun `rejects a TON address with the wrong length`() {
        assertFalse(WalletAddressValidator.isValid(ChainId.TON, "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8x"))
    }

    @Test
    fun `accepts a well-formed TRON address starting with T`() {
        assertTrue(WalletAddressValidator.isValid(ChainId.TRON, "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"))
    }

    @Test
    fun `rejects a TRON address not starting with T`() {
        assertFalse(WalletAddressValidator.isValid(ChainId.TRON, "1R7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"))
    }
}
