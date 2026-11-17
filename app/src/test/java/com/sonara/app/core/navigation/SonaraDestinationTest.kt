package com.sonara.app.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SonaraDestinationTest {

    @Test
    fun primaryDestinationsAreHomeLibraryPlaylistsFoldersInOrder() {
        assertEquals(
            listOf(
                SonaraDestination.Home,
                SonaraDestination.Library,
                SonaraDestination.Playlists,
                SonaraDestination.Folders,
            ),
            SonaraDestination.primary,
        )
    }

    @Test
    fun searchAndSettingsAreSecondary() {
        assertFalse(SonaraDestination.Search.isPrimary)
        assertFalse(SonaraDestination.Settings.isPrimary)
    }

    @Test
    fun routesAreUnique() {
        val routes = SonaraDestination.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun startDestinationIsPrimary() {
        assertEquals(SonaraDestination.Home, SonaraDestination.start)
        assertTrue(SonaraDestination.start.isPrimary)
    }

    @Test
    fun fromRouteResolvesKnownRoutesOnly() {
        SonaraDestination.entries.forEach {
            assertEquals(it, SonaraDestination.fromRoute(it.route))
        }
        assertNull(SonaraDestination.fromRoute("does-not-exist"))
        assertNull(SonaraDestination.fromRoute(null))
    }
}
