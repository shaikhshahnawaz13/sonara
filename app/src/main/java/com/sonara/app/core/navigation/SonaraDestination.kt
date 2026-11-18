package com.sonara.app.core.navigation

/**
 * Every navigable destination in SONARA (spec section 4).
 *
 * Primary destinations are the four pages of the tab pager and appear in the bottom navigation. They
 * are pages, not NavHost routes: the NavHost only knows [TABS_ROUTE] (the pager), Search and Settings.
 * Full Player, Queue, Lyrics, Equalizer, History and Sleep Timer are added by the milestones that build them.
 */
enum class SonaraDestination(
    val route: String,
    val label: String,
    val isPrimary: Boolean,
) {
    Home(route = "home", label = "Home", isPrimary = true),
    Library(route = "library", label = "Library", isPrimary = true),
    Playlists(route = "playlists", label = "Playlists", isPrimary = true),
    Folders(route = "folders", label = "Folders", isPrimary = true),
    Search(route = "search", label = "Search", isPrimary = false),
    Settings(route = "settings", label = "Settings", isPrimary = false);

    companion object {
        /** The single NavHost route that hosts the tab pager. */
        const val TABS_ROUTE = "tabs"

        val start: SonaraDestination = Home

        /** The four bottom-navigation destinations, in display order. */
        val primary: List<SonaraDestination> = entries.filter { it.isPrimary }

        fun fromRoute(route: String?): SonaraDestination? = entries.firstOrNull { it.route == route }
    }
}
