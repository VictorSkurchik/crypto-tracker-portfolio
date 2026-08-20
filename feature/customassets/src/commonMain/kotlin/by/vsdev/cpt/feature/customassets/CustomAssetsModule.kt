package by.vsdev.cpt.feature.customassets

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val customAssetsFeatureModule =
    module {
        viewModel { CustomAssetsViewModel(get()) }
    }
