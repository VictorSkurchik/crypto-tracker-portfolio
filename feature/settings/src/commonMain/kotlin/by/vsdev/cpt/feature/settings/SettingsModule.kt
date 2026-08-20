package by.vsdev.cpt.feature.settings

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsFeatureModule =
    module {
        viewModel { SettingsViewModel(get()) }
    }
