package by.vsdev.cpt.core.secrets

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AndroidSecretStore(
    context: Context,
) : SecretStore {
    private val masterKey =
        MasterKey
            .Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val prefs =
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    override suspend fun store(
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).apply()
    }

    override suspend fun retrieve(key: String): String? = prefs.getString(key, null)

    override suspend fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    private companion object {
        const val PREFS_FILE_NAME = "cpt_secrets"
    }
}
