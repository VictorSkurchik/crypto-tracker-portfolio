package by.vsdev.cpt.app.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import by.vsdev.cpt.core.designsystem.CptTheme
import by.vsdev.cpt.core.navigation.CustomAssetsRoute
import by.vsdev.cpt.core.navigation.ExchangesRoute
import by.vsdev.cpt.core.navigation.PortfolioRoute
import by.vsdev.cpt.core.navigation.SettingsRoute
import by.vsdev.cpt.core.navigation.WalletsRoute
import by.vsdev.cpt.feature.customassets.customAssetsGraph
import by.vsdev.cpt.feature.exchanges.exchangesGraph
import by.vsdev.cpt.feature.portfolio.portfolioGraph
import by.vsdev.cpt.feature.settings.settingsGraph
import by.vsdev.cpt.feature.wallets.walletsGraph

private data class TopLevelDestination(
    val route: Any,
    val label: String,
)

private val topLevelDestinations =
    listOf(
        TopLevelDestination(PortfolioRoute, "Portfolio"),
        TopLevelDestination(WalletsRoute, "Wallets"),
        TopLevelDestination(ExchangesRoute, "Exchanges"),
        TopLevelDestination(CustomAssetsRoute, "Custom"),
        TopLevelDestination(SettingsRoute, "Settings"),
    )

@Composable
fun CptApp() {
    CptTheme {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        val selected =
                            currentDestination?.hierarchy?.any {
                                it.route == destination.route::class.qualifiedName
                            } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {},
                            label = { Text(destination.label) },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = PortfolioRoute,
                modifier = Modifier.padding(padding),
            ) {
                portfolioGraph()
                walletsGraph()
                exchangesGraph()
                customAssetsGraph()
                settingsGraph()
            }
        }
    }
}
