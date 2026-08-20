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
 * Statically verifies that every binding declared by the three Koin modules :core:di itself
 * assembles ([networkModule], [databaseModule], [dataModule] — the ones [initKoin] always wires,
 * as opposed to the per-app `platformModule`/`featureModules`) has its constructor dependencies
 * satisfied by some other binding, so a missing or mistyped `single { ... }` fails this build
 * instead of only surfacing as a runtime crash for a real user.
 *
 * [SecretStore] and [DatabaseProvider] are only ever supplied by each app's own `platformModule`
 * (Android/Desktop/iOS secret storage and Room builder respectively) — out of this module's
 * scope — so they're declared via [extraTypes] rather than faked here. [OnChainProvider] and
 * [ExchangeConnector] are likewise whitelisted: each concrete provider/connector is registered as
 * a `List<OnChainProvider>`/`List<ExchangeConnector>` constructor argument of its registry, never
 * as its own Koin definition, so there is no "real" binding for the interface type to find.
 * [HttpClientEngine] is whitelisted too: Ktor's `HttpClient` constructor takes one, but which
 * engine to use is resolved automatically per-platform (see `createHttpClient`), never bound via
 * Koin.
 *
 * Koin's reflection-based `verify()` (`org.koin.test.verify`) requires `kotlin-reflect`, which is
 * only present for the JVM target here — hence this test lives in `jvmTest`, not `commonTest`.
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
