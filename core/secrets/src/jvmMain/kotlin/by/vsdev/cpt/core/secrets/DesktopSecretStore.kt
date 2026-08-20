package by.vsdev.cpt.core.secrets

import java.io.File
import java.security.SecureRandom
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Desktop has no universal OS-keyring API from pure JVM, so this is the weakest of the three
 * platform implementations: values are AES-256-GCM encrypted with a key generated on first run
 * and stored alongside the ciphertext in a file under the user's home directory with
 * owner-only permissions. Worth revisiting (e.g. an OS-keyring-integration library) if Desktop
 * use grows beyond personal/single-machine use.
 */
@OptIn(ExperimentalEncodingApi::class)
class DesktopSecretStore(
    appDataDir: File = File(System.getProperty("user.home"), ".crypto-portfolio-tracker"),
) : SecretStore {
    private val storeFile = File(appDataDir, "secrets.properties")
    private val keyFile = File(appDataDir, "secrets.key")

    init {
        appDataDir.mkdirs()
    }

    private val secretKey: SecretKeySpec by lazy {
        if (!keyFile.exists()) {
            val bytes = ByteArray(AES_256_KEY_BYTES)
            SecureRandom().nextBytes(bytes)
            keyFile.writeBytes(bytes)
            runCatching {
                keyFile.setReadable(false, false)
                keyFile.setReadable(true, true)
                keyFile.setWritable(false, false)
                keyFile.setWritable(true, true)
            }
        }
        SecretKeySpec(keyFile.readBytes(), "AES")
    }

    override suspend fun store(
        key: String,
        value: String,
    ) {
        val properties = loadProperties()
        properties.setProperty(key, encrypt(value))
        saveProperties(properties)
    }

    override suspend fun retrieve(key: String): String? {
        val encrypted = loadProperties().getProperty(key) ?: return null
        return decrypt(encrypted)
    }

    override suspend fun remove(key: String) {
        val properties = loadProperties()
        properties.remove(key)
        saveProperties(properties)
    }

    private fun loadProperties(): Properties =
        Properties().apply {
            if (storeFile.exists()) storeFile.inputStream().use { load(it) }
        }

    private fun saveProperties(properties: Properties) {
        storeFile.outputStream().use { properties.store(it, null) }
        runCatching {
            storeFile.setReadable(false, false)
            storeFile.setReadable(true, true)
            storeFile.setWritable(false, false)
            storeFile.setWritable(true, true)
        }
    }

    private fun encrypt(plaintext: String): String {
        val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encode(iv) + ":" + Base64.encode(ciphertext)
    }

    private fun decrypt(encoded: String): String? {
        val parts = encoded.split(":", limit = 2)
        if (parts.size != 2) return null
        val iv = Base64.decode(parts[0])
        val ciphertext = Base64.decode(parts[1])
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private companion object {
        const val AES_256_KEY_BYTES = 32
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
