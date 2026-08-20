package by.vsdev.cpt.feature.settings.fakes

import by.vsdev.cpt.core.secrets.SecretStore

// AppPreferences is a concrete class, so its SecretStore is faked here instead of itself.
class FakeSecretStore : SecretStore {
    private val values = mutableMapOf<String, String>()

    override suspend fun store(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override suspend fun retrieve(key: String): String? = values[key]

    override suspend fun remove(key: String) {
        values.remove(key)
    }
}
