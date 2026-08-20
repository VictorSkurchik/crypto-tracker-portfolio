package by.vsdev.cpt.feature.exchanges

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val exchangesFeatureModule =
    module {
        viewModel { ExchangesViewModel(get(), get()) }
    }
