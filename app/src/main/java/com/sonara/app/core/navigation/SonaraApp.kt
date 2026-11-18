package com.sonara.app.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.SonaraBottomNavigation
import com.sonara.app.core.design.components.SonaraNavItem
import com.sonara.app.core.design.components.SonaraScreenSurface
import com.sonara.app.feature.search.SearchScreen
import com.sonara.app.feature.settings.SettingsScreen

private val primaryNavItems: List<SonaraNavItem> =
    SonaraDestination.primary.map { SonaraNavItem(key = it.route, label = it.label) }

/**
 * Root composable: scaffold, bottom navigation and the navigation graph. Holds no playback state.
 *
 * The NavHost has three destinations: the tab pager, Search and Settings. The pager's state lives here,
 * above the NavHost, so the selected tab and the navigation indicator survive opening and closing
 * deeper screens.
 */
@Composable
fun SonaraApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val onTabs = backStackEntry?.destination?.route == SonaraDestination.TABS_ROUTE
    val motion = SonaraTheme.motion
    val tabs = rememberSonaraTabsState()

    // Back from any tab other than Home returns to Home, then leaves the app, as before.
    BackHandler(enabled = onTabs && tabs.selectedPage != 0) {
        tabs.select(0, motion)
    }

    Scaffold(
        containerColor = SonaraTheme.colors.background,
        bottomBar = {
            if (onTabs) {
                SonaraBottomNavigation(
                    items = primaryNavItems,
                    selectedIndex = tabs.selectedPage,
                    indicatorPosition = { tabs.position },
                    onSelect = { index -> tabs.select(index, motion) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SonaraDestination.TABS_ROUTE,
            modifier = Modifier.padding(innerPadding),
            // Explicit on purpose: the library default is a crossfade, which SONARA never uses.
            enterTransition = { motion.pushEnter() },
            exitTransition = { motion.pushExit() },
            popEnterTransition = { motion.popEnter() },
            popExitTransition = { motion.popExit() },
        ) {
            composable(SonaraDestination.TABS_ROUTE) {
                SonaraTabsHost(
                    state = tabs,
                    onOpenSearch = { navController.navigate(SonaraDestination.Search.route) },
                    onOpenSettings = { navController.navigate(SonaraDestination.Settings.route) },
                )
            }
            composable(SonaraDestination.Search.route) {
                SonaraScreenSurface { SearchScreen(onBack = { navController.popBackStack() }) }
            }
            composable(SonaraDestination.Settings.route) {
                SonaraScreenSurface { SettingsScreen(onBack = { navController.popBackStack() }) }
            }
        }
    }
}
