package by.vsdev.cpt.app.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import by.vsdev.cpt.core.designsystem.CptFoldPosture
import by.vsdev.cpt.core.designsystem.CptNavIcons
import by.vsdev.cpt.core.designsystem.CptTheme
import by.vsdev.cpt.core.designsystem.CptWindowWidthSizeClass
import by.vsdev.cpt.core.designsystem.rememberFoldPosture
import by.vsdev.cpt.core.designsystem.rememberWindowWidthSizeClass
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
    val shortLabel: String,
    val fullLabel: String,
    val icon: @Composable () -> Unit,
)

private val topLevelDestinations =
    listOf(
        TopLevelDestination(PortfolioRoute, "Portfolio", "Portfolio") { CptNavIcons.Portfolio() },
        TopLevelDestination(WalletsRoute, "Wallets", "Wallets") { CptNavIcons.Wallets() },
        TopLevelDestination(ExchangesRoute, "Exchanges", "Exchanges") { CptNavIcons.Exchanges() },
        TopLevelDestination(CustomAssetsRoute, "Assets", "Custom Assets") { CptNavIcons.CustomAssets() },
        TopLevelDestination(SettingsRoute, "Settings", "Settings") { CptNavIcons.Settings() },
    )

private val MAX_CONTENT_WIDTH = 640.dp
private val NAV_RAIL_WIDTH = 88.dp
private val NAV_DRAWER_WIDTH = 280.dp

@Composable
fun CptApp() {
    CptTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination

        val windowSizeClass = rememberWindowWidthSizeClass()
        val foldPosture = rememberFoldPosture()

        val isSelected: (TopLevelDestination) -> Boolean = { destination ->
            currentDestination.isTopLevelDestinationInHierarchy(destination)
        }
        val onNavigate: (Any) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        when {
            foldPosture is CptFoldPosture.HalfOpened && foldPosture.isHingeVertical ->
                CptAppSidePanelNav(foldPosture, isSelected, onNavigate) { modifier -> AppNavHost(navController, modifier) }
            windowSizeClass == CptWindowWidthSizeClass.EXPANDED ->
                CptAppPermanentDrawer(isSelected, onNavigate) { modifier -> AppNavHost(navController, modifier) }
            windowSizeClass == CptWindowWidthSizeClass.MEDIUM ->
                CptAppSidePanelNav(CptFoldPosture.None, isSelected, onNavigate) { modifier -> AppNavHost(navController, modifier) }
            else ->
                CptAppBottomNav(isSelected, onNavigate) { modifier -> AppNavHost(navController, modifier) }
        }
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.route == destination.route::class.qualifiedName } == true

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = PortfolioRoute, modifier = modifier) {
        portfolioGraph()
        walletsGraph()
        exchangesGraph()
        customAssetsGraph()
        settingsGraph()
    }
}

@Composable
private fun CptAppBottomNav(
    isSelected: (TopLevelDestination) -> Boolean,
    onNavigate: (Any) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = isSelected(destination),
                        onClick = { onNavigate(destination.route) },
                        icon = destination.icon,
                        label = { Text(destination.shortLabel) },
                        colors = navItemColors(),
                    )
                }
            }
        },
    ) { padding -> content(Modifier.padding(padding)) }
}

@Composable
private fun navItemColors() =
    NavigationBarItemDefaults.colors(
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

@Composable
private fun railItemColors() =
    NavigationRailItemDefaults.colors(
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

@Composable
private fun drawerItemColors() =
    NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

/**
 * Used for medium/expanded windows and — with a real [CptFoldPosture.HalfOpened] posture — for a
 * vertically hinged foldable: the rail occupies the pane to the left of the hinge and a spacer
 * matching the hinge's own width keeps content from rendering underneath the seam.
 */
@Composable
private fun CptAppSidePanelNav(
    foldPosture: CptFoldPosture,
    isSelected: (TopLevelDestination) -> Boolean,
    onNavigate: (Any) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val hingeGapWidth =
            (foldPosture as? CptFoldPosture.HalfOpened)?.let { posture ->
                maxWidth * (posture.hingeEndFraction - posture.hingeStartFraction)
            }
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(modifier = Modifier.width(NAV_RAIL_WIDTH)) {
                topLevelDestinations.forEach { destination ->
                    NavigationRailItem(
                        selected = isSelected(destination),
                        onClick = { onNavigate(destination.route) },
                        icon = destination.icon,
                        label = { Text(destination.shortLabel) },
                        colors = railItemColors(),
                    )
                }
            }
            if (hingeGapWidth != null) {
                Spacer(modifier = Modifier.width(hingeGapWidth))
            }
            content(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CptAppPermanentDrawer(
    isSelected: (TopLevelDestination) -> Boolean,
    onNavigate: (Any) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(modifier = Modifier.width(NAV_DRAWER_WIDTH)) {
                topLevelDestinations.forEach { destination ->
                    NavigationDrawerItem(
                        selected = isSelected(destination),
                        onClick = { onNavigate(destination.route) },
                        icon = destination.icon,
                        label = { Text(destination.fullLabel) },
                        colors = drawerItemColors(),
                    )
                }
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            content(Modifier.widthIn(max = MAX_CONTENT_WIDTH))
        }
    }
}
