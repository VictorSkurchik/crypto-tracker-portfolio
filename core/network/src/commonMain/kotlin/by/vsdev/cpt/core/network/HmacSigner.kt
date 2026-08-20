package by.vsdev.cpt.core.network

import org.kotlincrypto.macs.hmac.sha2.HmacSHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * HMAC-SHA256 request signing, needed for every exchange connector. Uses KotlinCrypto's pure-Kotlin
 * implementation rather than `javax.crypto.Mac` because signing now happens on-device on every
 * client platform including iOS, where `javax.crypto` doesn't exist.
 */
@OptIn(ExperimentalEncodingApi::class)
object HmacSigner {
    @Suppress("MagicNumber")
    fun hex(
        secret: String,
        message: String,
    ): String {
        val digest = HmacSHA256(secret.encodeToByteArray()).doFinal(message.encodeToByteArray())
        return digest.joinToString("") { byte -> ((byte.toInt() and 0xFF) + 0x100).toString(16).substring(1) }
    }

    fun base64(
        secret: String,
        message: String,
    ): String {
        val digest = HmacSHA256(secret.encodeToByteArray()).doFinal(message.encodeToByteArray())
        return Base64.encode(digest)
    }
}
