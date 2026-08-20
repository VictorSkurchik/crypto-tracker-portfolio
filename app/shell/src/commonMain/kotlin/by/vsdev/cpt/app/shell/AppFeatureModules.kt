package by.vsdev.cpt.app.shell

import by.vsdev.cpt.feature.customassets.customAssetsFeatureModule
import by.vsdev.cpt.feature.exchanges.exchangesFeatureModule
import by.vsdev.cpt.feature.portfolio.portfolioFeatureModule
import by.vsdev.cpt.feature.settings.settingsFeatureModule
import by.vsdev.cpt.feature.wallets.walletsFeatureModule
import org.koin.core.module.Module

/** Every feature module's Koin bindings (mainly ViewModels), collected once here since :core:di
 * can't depend on :feature:* modules without inverting the usual core->feature dependency direction. */
val appFeatureModules: List<Module> =
    listOf(
        portfolioFeatureModule,
        walletsFeatureModule,
        exchangesFeatureModule,
        customAssetsFeatureModule,
        settingsFeatureModule,
    )
