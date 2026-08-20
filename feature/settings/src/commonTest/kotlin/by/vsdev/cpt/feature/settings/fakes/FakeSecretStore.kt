package by.vsdev.cpt.feature.settings.fakes

import by.vsdev.cpt.core.secrets.SecretStore

/**
 * In-memory fake for the platform [SecretStore] `AppPreferences` depends on. `AppPreferences` is a
 * concrete class, not an interface, so testing `SettingsViewModel` means faking its dependency
 * rather than `AppPreferences` itself — mirroring the pattern `:feature:portfolio` uses for its own
 * `SecretStore` dependency.
 */
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
