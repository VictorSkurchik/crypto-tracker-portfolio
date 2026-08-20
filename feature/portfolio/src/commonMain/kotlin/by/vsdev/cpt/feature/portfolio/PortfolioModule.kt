package by.vsdev.cpt.feature.portfolio

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val portfolioFeatureModule =
    module {
        viewModel { PortfolioViewModel(get()) }
    }
