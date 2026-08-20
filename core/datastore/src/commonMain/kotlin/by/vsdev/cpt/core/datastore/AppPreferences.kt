package by.vsdev.cpt.core.datastore

import by.vsdev.cpt.core.secrets.SecretStore

/**
 * Small, non-secret app settings. Reuses :core:secrets' per-platform [SecretStore] as its backing
 * store rather than introducing a second cross-platform key-value mechanism (e.g. DataStore) for
 * what's just a couple of optional string preferences.
 */
class AppPreferences(
    private val secretStore: SecretStore,
) {
    suspend fun coinMarketCapApiKey(): String? = secretStore.retrieve(COIN_MARKET_CAP_API_KEY)

    suspend fun setCoinMarketCapApiKey(value: String?) {
        if (value.isNullOrBlank()) secretStore.remove(COIN_MARKET_CAP_API_KEY) else secretStore.store(COIN_MARKET_CAP_API_KEY, value)
    }

    suspend fun etherscanApiKey(): String? = secretStore.retrieve(ETHERSCAN_API_KEY)

    suspend fun setEtherscanApiKey(value: String?) {
        if (value.isNullOrBlank()) secretStore.remove(ETHERSCAN_API_KEY) else secretStore.store(ETHERSCAN_API_KEY, value)
    }

    private companion object {
        const val COIN_MARKET_CAP_API_KEY = "pref_coin_market_cap_api_key"
        const val ETHERSCAN_API_KEY = "pref_etherscan_api_key"
    }
}
