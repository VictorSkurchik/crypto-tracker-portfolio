package by.vsdev.cpt.feature.exchanges

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import by.vsdev.cpt.core.navigation.ExchangesRoute

fun NavGraphBuilder.exchangesGraph() {
    composable<ExchangesRoute> { ExchangesScreen() }
}
