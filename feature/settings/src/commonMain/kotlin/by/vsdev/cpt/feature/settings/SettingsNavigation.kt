package by.vsdev.cpt.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import by.vsdev.cpt.core.navigation.SettingsRoute

fun NavGraphBuilder.settingsGraph() {
    composable<SettingsRoute> { SettingsScreen() }
}
