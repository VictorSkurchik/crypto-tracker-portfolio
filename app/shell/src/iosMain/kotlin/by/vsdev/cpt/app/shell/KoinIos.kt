package by.vsdev.cpt.app.shell

import by.vsdev.cpt.core.database.DatabaseProvider
import by.vsdev.cpt.core.database.IosDatabaseProvider
import by.vsdev.cpt.core.di.initKoin
import by.vsdev.cpt.core.secrets.IosSecretStore
import by.vsdev.cpt.core.secrets.SecretStore
import org.koin.dsl.module

/** Called once from Swift's `iOSApp.init()` before any Compose UI is shown. */
fun doInitKoin() {
    val iosPlatformModule =
        module {
            single<SecretStore> { IosSecretStore() }
            single<DatabaseProvider> { IosDatabaseProvider() }
        }
    initKoin(iosPlatformModule, appFeatureModules)
}
