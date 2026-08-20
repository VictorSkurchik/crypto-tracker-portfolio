package by.vsdev.cpt

import android.app.Application
import android.content.Context
import by.vsdev.cpt.app.shell.appFeatureModules
import by.vsdev.cpt.core.database.AndroidDatabaseProvider
import by.vsdev.cpt.core.database.DatabaseProvider
import by.vsdev.cpt.core.di.initKoin
import by.vsdev.cpt.core.secrets.AndroidSecretStore
import by.vsdev.cpt.core.secrets.SecretStore
import org.koin.dsl.module

class CptApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val androidPlatformModule =
            module {
                single<Context> { this@CptApplication }
                single<SecretStore> { AndroidSecretStore(get()) }
                single<DatabaseProvider> { AndroidDatabaseProvider(get()) }
            }
        initKoin(androidPlatformModule, appFeatureModules)
    }
}
