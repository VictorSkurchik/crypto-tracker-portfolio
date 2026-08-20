package by.vsdev.cpt

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import by.vsdev.cpt.app.shell.CptApp
import by.vsdev.cpt.app.shell.appFeatureModules
import by.vsdev.cpt.core.database.DatabaseProvider
import by.vsdev.cpt.core.database.DesktopDatabaseProvider
import by.vsdev.cpt.core.di.initKoin
import by.vsdev.cpt.core.secrets.DesktopSecretStore
import by.vsdev.cpt.core.secrets.SecretStore
import org.koin.dsl.module

fun main() {
    val desktopPlatformModule =
        module {
            single<SecretStore> { DesktopSecretStore() }
            single<DatabaseProvider> { DesktopDatabaseProvider() }
        }
    initKoin(desktopPlatformModule, appFeatureModules)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Crypto Portfolio Tracker",
        ) {
            CptApp()
        }
    }
}
