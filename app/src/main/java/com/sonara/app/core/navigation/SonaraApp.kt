package com.sonara.app.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sonara.app.core.animation.SonaraMotion
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.SonaraBottomNavigation
import com.sonara.app.core.design.components.SonaraNavItem
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.design.components.LocalSnackbar
import com.sonara.app.core.design.components.SonaraMiniPlayer
import com.sonara.app.core.design.components.SonaraScreenSurface
import com.sonara.app.core.design.components.SonaraSnackbarHost
import com.sonara.app.core.design.components.SonaraSnackbarState
import com.sonara.app.data.model.Genre
import com.sonara.app.data.repository.FolderRepository
import com.sonara.app.feature.detail.AlbumDetailScreen
import com.sonara.app.feature.detail.ArtistDetailScreen
import com.sonara.app.feature.detail.FolderDetailScreen
import com.sonara.app.feature.detail.GenreDetailScreen
import com.sonara.app.feature.playlists.LocalPlaylistPicker
import com.sonara.app.feature.playlists.PlaylistDetailScreen
import com.sonara.app.feature.playlists.PlaylistSongPickerScreen
import com.sonara.app.feature.playlists.PlaylistPicker
import com.sonara.app.feature.playlists.PlaylistPickerHost
import com.sonara.app.feature.search.SearchScreen
import com.sonara.app.feature.settings.SettingsScreen

private val primaryNavItems: List<SonaraNavItem> =
    SonaraDestination.primary.map { SonaraNavItem(key = it.route, label = it.label) }

/**
 * Root composable: scaffold, bottom navigation and the navigation graph. Holds no playback state.
 *
 * The NavHost has the tab pager, Search, Settings and the detail screens (album, artist, genre, folder). The pager's state lives here,
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
    val snackbar = remember { SonaraSnackbarState() }
    val libraryCategory = remember { mutableStateOf(LibraryCategory.Songs) }
    val playlistPicker = remember { PlaylistPicker() }
    val currentMotion by rememberUpdatedState(motion)
    val navigator = remember(navController, tabs) {
        SonaraNavigator(libraryCategory, navController, tabs) { currentMotion }
    }

    TabsBackHandler(onTabs = onTabs, tabs = tabs, motion = motion)

    CompositionLocalProvider(
        LocalSonaraNavigator provides navigator,
        LocalSnackbar provides snackbar,
        LocalPlaylistPicker provides playlistPicker,
    ) {
    Scaffold(
        containerColor = SonaraTheme.colors.background,
        snackbarHost = { SonaraSnackbarHost(snackbar) },
        bottomBar = {
            Column {
                AppMiniPlayer(applyNavigationInset = !onTabs)
                if (onTabs) {
                    SonaraBottomNavigation(
                        items = primaryNavItems,
                        selectedIndex = tabs.selectedPage,
                        indicatorPosition = { tabs.position },
                        onSelect = { index -> tabs.select(index, motion) },
                    )
                }
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
                // Tabs are "settled" when no push or pop is moving them; only then are the other pages composed.
                val settled = remember(transition) {
                    derivedStateOf {
                        transition.currentState == EnterExitState.Visible &&
                            transition.targetState == EnterExitState.Visible
                    }
                }
                SonaraTabsHost(state = tabs, settled = { settled.value })
            }
            composable(SonaraDestination.Search.route) {
                SonaraScreenSurface { SearchScreen() }
            }
            composable(SonaraDestination.Settings.route) {
                SonaraScreenSurface { SettingsScreen(onBack = { navController.popBackStack() }) }
            }
            composable(
                route = SonaraRoutes.ALBUM,
                arguments = listOf(navArgument(SonaraRoutes.ARG_ALBUM_ID) { type = NavType.LongType }),
            ) { entry ->
                val albumId = entry.arguments?.getLong(SonaraRoutes.ARG_ALBUM_ID) ?: return@composable
                SonaraScreenSurface { AlbumDetailScreen(albumId) }
            }
            composable(
                route = SonaraRoutes.PLAYLIST,
                arguments = listOf(navArgument(SonaraRoutes.ARG_PLAYLIST_ID) { type = NavType.LongType }),
            ) { entry ->
                val playlistId = entry.arguments?.getLong(SonaraRoutes.ARG_PLAYLIST_ID) ?: return@composable
                SonaraScreenSurface { PlaylistDetailScreen(playlistId) }
            }
            composable(
                route = SonaraRoutes.PLAYLIST_ADD,
                arguments = listOf(navArgument(SonaraRoutes.ARG_PLAYLIST_ID) { type = NavType.LongType }),
            ) { entry ->
                val playlistId = entry.arguments?.getLong(SonaraRoutes.ARG_PLAYLIST_ID) ?: return@composable
                SonaraScreenSurface { PlaylistSongPickerScreen(playlistId) }
            }
            composable(
                route = SonaraRoutes.ARTIST,
                arguments = listOf(navArgument(SonaraRoutes.ARG_NAME) { type = NavType.StringType }),
            ) { entry ->
                val name = entry.arguments?.getString(SonaraRoutes.ARG_NAME) ?: return@composable
                SonaraScreenSurface { ArtistDetailScreen(name) }
            }
            composable(
                route = SonaraRoutes.GENRE,
                arguments = listOf(navArgument(SonaraRoutes.ARG_NAME) { type = NavType.StringType }),
            ) { entry ->
                val key = entry.arguments?.getString(SonaraRoutes.ARG_NAME) ?: return@composable
                SonaraScreenSurface { GenreDetailScreen(if (key == Genre.UNKNOWN_KEY) null else key) }
            }
            composable(
                route = SonaraRoutes.FOLDER,
                arguments = listOf(
                    navArgument(SonaraRoutes.ARG_PATH) { type = NavType.StringType },
                    navArgument(SonaraRoutes.ARG_RECURSIVE) { type = NavType.BoolType; defaultValue = false },
                ),
            ) { entry ->
                val path = entry.arguments?.getString(SonaraRoutes.ARG_PATH) ?: return@composable
                val recursive = entry.arguments?.getBoolean(SonaraRoutes.ARG_RECURSIVE) ?: false
                SonaraScreenSurface { FolderDetailScreen(FolderRepository.prefixFromRoute(path), recursive) }
            }
        }
    }
    PlaylistPickerHost(playlistPicker)
    }
}

/**
 * Back from any tab other than Home returns to Home, then leaves the app. The selected page is read here,
 * in its own scope, so the page changing half-way through a tab trip recomposes this and not the whole app.
 */
@Composable
private fun TabsBackHandler(onTabs: Boolean, tabs: SonaraTabsState, motion: SonaraMotion) {
    BackHandler(enabled = onTabs && tabs.selectedPage != 0) {
        tabs.select(0, motion)
    }
}

/** Basic now-playing bar above the navigation; shown only while the playback service has a track loaded. */
@Composable
private fun AppMiniPlayer(applyNavigationInset: Boolean) {
    val controller = LocalAppContainer.current.playbackController
    val state by controller.state.collectAsState()
    if (!state.connected || !state.hasMedia) return
    SonaraMiniPlayer(
        trackId = state.currentTrackId,
        title = state.title.orEmpty(),
        artist = state.artist.orEmpty(),
        isPlaying = state.isPlaying,
        canSkipNext = state.hasNext,
        onTogglePlayPause = controller::togglePlayPause,
        onSkipNext = controller::skipToNext,
        modifier = if (applyNavigationInset) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier,
    )
}
