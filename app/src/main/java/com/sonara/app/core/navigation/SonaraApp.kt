package com.sonara.app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.SonaraBottomNavigation
import com.sonara.app.core.design.components.SonaraNavItem
import com.sonara.app.feature.folders.FoldersScreen
import com.sonara.app.feature.home.HomeScreen
import com.sonara.app.feature.library.LibraryScreen
import com.sonara.app.feature.playlists.PlaylistsScreen
import com.sonara.app.feature.search.SearchScreen
import com.sonara.app.feature.settings.SettingsScreen

private val primaryNavItems: List<SonaraNavItem> =
    SonaraDestination.primary.map { SonaraNavItem(key = it.route, label = it.label) }

/** Root composable: scaffold, bottom navigation and the navigation graph. Holds no playback state. */
@Composable
fun SonaraApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = SonaraDestination.fromRoute(backStackEntry?.destination?.route)

    Scaffold(
        containerColor = SonaraTheme.colors.background,
        bottomBar = {
            if (current?.isPrimary == true) {
                SonaraBottomNavigation(
                    items = primaryNavItems,
                    selectedKey = current.route,
                    onSelect = { route ->
                        SonaraDestination.fromRoute(route)?.let { navController.navigateToPrimary(it) }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SonaraDestination.start.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(SonaraDestination.Home.route) {
                HomeScreen(
                    onOpenSearch = { navController.navigate(SonaraDestination.Search.route) },
                    onOpenSettings = { navController.navigate(SonaraDestination.Settings.route) },
                )
            }
            composable(SonaraDestination.Library.route) {
                LibraryScreen(
                    onOpenSearch = { navController.navigate(SonaraDestination.Search.route) },
                )
            }
            composable(SonaraDestination.Playlists.route) { PlaylistsScreen() }
            composable(SonaraDestination.Folders.route) { FoldersScreen() }
            composable(SonaraDestination.Search.route) {
                SearchScreen(onBack = { navController.popBackStack() })
            }
            composable(SonaraDestination.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/**
 * Switches between primary tabs without stacking duplicates and while preserving each tab's
 * saved state (scroll position, etc.), as required by spec section 4.
 */
private fun NavController.navigateToPrimary(destination: SonaraDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
