package com.sonara.app.core.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavController
import com.sonara.app.core.animation.SonaraMotion
import com.sonara.app.data.model.Genre

/** The sections of the Library screen. */
enum class LibraryCategory(val label: String) {
    Songs("SONGS"),
    Albums("ALBUMS"),
    Artists("ARTISTS"),
    Genres("GENRES"),
}

/** +1 when [to] comes after [from] (content slides in from the right), -1 when it comes before. */
internal fun categorySlideDirection(from: LibraryCategory, to: LibraryCategory): Int =
    if (to.ordinal > from.ordinal) 1 else -1

/**
 * Everything a screen may ask the app to open. Screens call these instead of owning a NavController,
 * so they stay independent of how navigation is wired.
 */
@Stable
class SonaraNavigator(
    /** The Library section currently selected; kept above the pager so it survives tab changes. */
    val libraryCategory: MutableState<LibraryCategory>,
    private val navController: NavController,
    private val tabs: SonaraTabsState,
    private val motion: () -> SonaraMotion,
) {
    fun back() { navController.popBackStack() }

    fun openSearch() = push(SonaraDestination.Search.route)
    fun openSettings() = push(SonaraDestination.Settings.route)
    fun openAlbum(albumId: Long) = push(SonaraRoutes.album(albumId))
    fun openPlaylist(playlistId: Long) = push(SonaraRoutes.playlist(playlistId))
    fun openArtist(name: String) = push(SonaraRoutes.artist(name))
    fun openGenre(name: String?) = push(SonaraRoutes.genre(name ?: Genre.UNKNOWN_KEY))
    fun openFolder(path: String, recursive: Boolean) = push(SonaraRoutes.folder(path, recursive))

    /** Switches to the Library tab and selects [category]. From a deeper screen this returns to the tabs first. */
    fun openLibrary(category: LibraryCategory) {
        libraryCategory.value = category
        openTab(SonaraDestination.Library)
    }

    fun openTab(destination: SonaraDestination) {
        val index = SonaraDestination.primary.indexOf(destination)
        if (index < 0) return
        // Pop back to the tabs if a deeper screen is showing (Search, details).
        navController.popBackStack(SonaraDestination.TABS_ROUTE, inclusive = false)
        tabs.select(index, motion())
    }

    private fun push(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }
}

val LocalSonaraNavigator = compositionLocalOf<SonaraNavigator> {
    error("SonaraNavigator not provided")
}
