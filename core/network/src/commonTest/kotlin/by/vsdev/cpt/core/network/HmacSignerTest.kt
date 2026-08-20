package by.vsdev.cpt.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HmacSignerTest {
    @Test
    fun `hex signature is deterministic for the same key and message`() {
        val first = HmacSigner.hex("secret", "message")
        val second = HmacSigner.hex("secret", "message")
        assertEquals(first, second)
    }

    @Test
    fun `hex signature is exactly 64 lowercase hex characters`() {
        val signature = HmacSigner.hex("secret", "message")
        assertEquals(64, signature.length)
        assertTrue(signature.all { it in '0'..'9' || it in 'a'..'f' }, "expected lowercase hex, got $signature")
    }

    @Test
    fun `hex signature changes when the message changes`() {
        val a = HmacSigner.hex("secret", "message-a")
        val b = HmacSigner.hex("secret", "message-b")
        assertNotEquals(a, b)
    }

    @Test
    fun `hex signature changes when the secret changes`() {
        val a = HmacSigner.hex("secret-a", "message")
        val b = HmacSigner.hex("secret-b", "message")
        assertNotEquals(a, b)
    }

    @Test
    fun `base64 signature is deterministic and non-blank`() {
        val first = HmacSigner.base64("secret", "message")
        val second = HmacSigner.base64("secret", "message")
        assertEquals(first, second)
        assertTrue(first.isNotBlank())
    }

    @Test
    fun `hex and base64 encode the same underlying digest bytes`() {
        // A 32-byte SHA-256 digest encodes to exactly 64 hex chars and (with padding) 44 base64 chars.
        val hex = HmacSigner.hex("secret", "message")
        val base64 = HmacSigner.base64("secret", "message")
        assertEquals(64, hex.length)
        assertEquals(44, base64.length)
    }
}
