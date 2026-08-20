package by.vsdev.cpt.feature.portfolio

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import by.vsdev.cpt.core.navigation.PortfolioRoute

fun NavGraphBuilder.portfolioGraph() {
    composable<PortfolioRoute> { PortfolioScreen() }
}
