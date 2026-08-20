package by.vsdev.cpt.feature.wallets

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import by.vsdev.cpt.core.navigation.WalletsRoute

fun NavGraphBuilder.walletsGraph() {
    composable<WalletsRoute> { WalletsScreen() }
}
