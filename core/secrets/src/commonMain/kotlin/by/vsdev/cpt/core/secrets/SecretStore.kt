package by.vsdev.cpt.core.secrets

/**
 * Platform-appropriate secure storage for exchange API key/secret/passphrase values.
 * There is no backend to centralize this in this edition, so each platform backs it with the
 * closest thing to OS-level secret storage it has: Android Keystore-backed EncryptedSharedPreferences,
 * iOS Keychain, and (weakest of the three) a locally-encrypted file on Desktop.
 */
interface SecretStore {
    suspend fun store(
        key: String,
        value: String,
    )

    suspend fun retrieve(key: String): String?

    suspend fun remove(key: String)
}
