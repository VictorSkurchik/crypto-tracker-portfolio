package by.vsdev.cpt.feature.wallets

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val walletsFeatureModule =
    module {
        viewModel { WalletsViewModel(get()) }
    }
