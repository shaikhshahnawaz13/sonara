package com.sonara.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
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
    val motion = SonaraTheme.motion

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
            // Explicit on purpose: the library default is a crossfade, which SONARA never uses.
            enterTransition = {
                motion.enterFor(classifyNavTransition(initialState.routeOrNull(), targetState.routeOrNull(), backward = false))
            },
            exitTransition = {
                motion.exitFor(classifyNavTransition(initialState.routeOrNull(), targetState.routeOrNull(), backward = false))
            },
            popEnterTransition = {
                motion.enterFor(classifyNavTransition(initialState.routeOrNull(), targetState.routeOrNull(), backward = true))
            },
            popExitTransition = {
                motion.exitFor(classifyNavTransition(initialState.routeOrNull(), targetState.routeOrNull(), backward = true))
            },
        ) {
            composable(SonaraDestination.Home.route) {
                ScreenSurface {
                    HomeScreen(
                        onOpenSearch = { navController.navigate(SonaraDestination.Search.route) },
                        onOpenSettings = { navController.navigate(SonaraDestination.Settings.route) },
                    )
                }
            }
            composable(SonaraDestination.Library.route) {
                ScreenSurface {
                    LibraryScreen(
                        onOpenSearch = { navController.navigate(SonaraDestination.Search.route) },
                    )
                }
            }
            composable(SonaraDestination.Playlists.route) { ScreenSurface { PlaylistsScreen() } }
            composable(SonaraDestination.Folders.route) { ScreenSurface { FoldersScreen() } }
            composable(SonaraDestination.Search.route) {
                ScreenSurface { SearchScreen(onBack = { navController.popBackStack() }) }
            }
            composable(SonaraDestination.Settings.route) {
                ScreenSurface { SettingsScreen(onBack = { navController.popBackStack() }) }
            }
        }
    }
}

private fun NavBackStackEntry.routeOrNull(): String? = destination.route

/**
 * Every destination paints its own opaque background. Slides never reveal the previous screen through
 * the new one, and nothing depends on alpha. It uses the same colour the scaffold already had, so
 * nothing changes visually.
 */
@Composable
private fun ScreenSurface(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(SonaraTheme.colors.background)) {
        content()
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
