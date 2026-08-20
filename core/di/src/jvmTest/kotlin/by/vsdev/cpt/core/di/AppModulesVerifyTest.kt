package by.vsdev.cpt.core.di

import by.vsdev.cpt.core.database.DatabaseProvider
import by.vsdev.cpt.core.model.ExchangeConnector
import by.vsdev.cpt.core.model.OnChainProvider
import by.vsdev.cpt.core.secrets.SecretStore
import io.ktor.client.engine.HttpClientEngine
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

/**
 * Fails the build if a `single { ... }` in [networkModule]/[databaseModule]/[dataModule] is
 * missing or mistyped, instead of only surfacing as a runtime crash.
 *
 * [extraTypes] whitelists types never bound directly via Koin: [SecretStore]/[DatabaseProvider]
 * come from each app's own `platformModule`, out of this module's scope; [OnChainProvider]/
 * [ExchangeConnector] are only ever registered as `List<...>` constructor args of their registry;
 * [HttpClientEngine] is resolved per-platform by `createHttpClient`, not bound via Koin.
 *
 * In `jvmTest` (not `commonTest`) because `verify()` needs `kotlin-reflect`, JVM-only here.
 */
class AppModulesVerifyTest {
    @Test
    fun `core di modules resolve every constructor dependency`() {
        val combined = module { includes(networkModule, databaseModule, dataModule) }

        combined.verify(
            extraTypes =
                listOf(
                    SecretStore::class,
                    DatabaseProvider::class,
                    OnChainProvider::class,
                    ExchangeConnector::class,
                    HttpClientEngine::class,
                ),
        )
    }
}
