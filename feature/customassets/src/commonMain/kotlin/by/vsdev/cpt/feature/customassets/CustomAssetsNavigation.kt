package by.vsdev.cpt.feature.customassets

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import by.vsdev.cpt.core.navigation.CustomAssetsRoute

fun NavGraphBuilder.customAssetsGraph() {
    composable<CustomAssetsRoute> { CustomAssetsScreen() }
}
