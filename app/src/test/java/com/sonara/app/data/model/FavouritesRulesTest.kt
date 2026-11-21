package com.sonara.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavouritesRulesTest {
    @Test
    fun tappingATrackNotInFavouritesAddsIt() {
        assertEquals(FavouriteChange.Add, PlaylistRules.favouriteChange(emptySet(), 7L))
        assertEquals(FavouriteChange.Add, PlaylistRules.favouriteChange(listOf(1L, 2L), 7L))
    }

    @Test
    fun tappingATrackAlreadyInFavouritesRemovesIt() {
        assertEquals(FavouriteChange.Remove, PlaylistRules.favouriteChange(setOf(7L, 9L), 7L))
    }

    @Test
    fun twoTapsReturnToTheStartingMembership() {
        var held = setOf(1L)
        repeat(2) {
            held = when (PlaylistRules.favouriteChange(held, 5L)) {
                FavouriteChange.Add -> held + 5L
                FavouriteChange.Remove -> held - 5L
            }
        }
        assertEquals(setOf(1L), held)
    }

    @Test
    fun addingNeverProducesADuplicate() {
        // Even if the heart is tapped against stale state, the append plan drops tracks already held.
        assertEquals(emptyList<Long>(), PlaylistRules.planAppend(listOf(7L), listOf(7L)))
        assertEquals(listOf(8L), PlaylistRules.planAppend(listOf(7L), listOf(7L, 8L, 8L)))
    }

    @Test
    fun favouritesNameIsMatchedIgnoringCaseAndSpelling() {
        assertTrue(PlaylistRules.isFavouritesName("Favourites"))
        assertTrue(PlaylistRules.isFavouritesName("  favourite "))
        assertTrue(PlaylistRules.isFavouritesName("FAVORITES"))
        assertFalse(PlaylistRules.isFavouritesName("Road trip"))
        assertTrue(PlaylistRules.isFavouritesName(PlaylistRules.FAVOURITES_DEFAULT_NAME))
        assertEquals(PlaylistRules.FAVOURITES_DEFAULT_NAME, PlaylistRules.FAVOURITES_NAMES.first())
    }

    // ---- M5.2: permanent, system-managed identity ----

    @Test
    fun favouriteHasAStableIdThatGeneratedIdsNeverUse() {
        // Room's generated ids start at 1, so a fixed negative id can never collide with a user's playlist.
        assertTrue(PlaylistRules.FAVOURITES_ID < 0)
        assertTrue(PlaylistRules.isSystem(PlaylistRules.FAVOURITES_ID))
        assertFalse(PlaylistRules.isSystem(1L))
        assertFalse(PlaylistRules.isSystem(0L))
        assertEquals(-1L, PlaylistRules.FAVOURITES_ID)
    }

    @Test
    fun favouriteIsCalledFavouriteAndNeverRenamedOrDeleted() {
        assertEquals("Favourite", PlaylistRules.FAVOURITES_DEFAULT_NAME)
        assertFalse(PlaylistRules.canRename(PlaylistRules.FAVOURITES_ID))
        assertFalse(PlaylistRules.canDelete(PlaylistRules.FAVOURITES_ID))
        assertTrue(PlaylistRules.canRename(7L))
        assertTrue(PlaylistRules.canDelete(7L))
    }

    @Test
    fun noOtherPlaylistCanTakeTheName() {
        // The system playlist is always in the list of existing names, so a second "Favourite" is refused.
        val existing = listOf(PlaylistRules.FAVOURITES_DEFAULT_NAME, "Gym")
        assertEquals(PlaylistNameError.Duplicate, PlaylistRules.validateName("favourite", existing))
        assertEquals(PlaylistNameError.Duplicate, PlaylistRules.validateName(" FAVOURITE ", existing))
    }

    @Test
    fun olderFavouritesPlaylistsMergeWithoutRepeatsAndKeepOrder() {
        val merged = PlaylistRules.mergeLegacyFavourites(
            existing = listOf(5L),
            legacy = listOf(listOf(1L, 2L, 5L), listOf(2L, 3L)),
        )
        assertEquals(listOf(1L, 2L, 3L), merged)
    }

    @Test
    fun mergingNothingChangesNothing() {
        assertEquals(emptyList<Long>(), PlaylistRules.mergeLegacyFavourites(listOf(1L), emptyList()))
        assertEquals(emptyList<Long>(), PlaylistRules.mergeLegacyFavourites(listOf(1L, 2L), listOf(listOf(2L, 1L))))
    }

    @Test
    fun rapidTapsAlternateAndNeverDuplicate() {
        // Each tap decides from what is stored at that moment, so any number of taps ends where parity says.
        var held = emptyList<Long>()
        repeat(7) {
            held = when (PlaylistRules.favouriteChange(held, 9L)) {
                FavouriteChange.Add -> PlaylistRules.planAppend(held, listOf(9L)).let { held + it }
                FavouriteChange.Remove -> held - 9L
            }
            assertTrue(held.count { it == 9L } <= 1)
        }
        assertEquals(listOf(9L), held)
    }

    @Test
    fun theSummaryKnowsItIsTheSystemPlaylist() {
        val summary = PlaylistSummary(PlaylistRules.FAVOURITES_ID, "Favourite", null, 0, 0, 0L, null)
        assertTrue(summary.isSystem)
        assertFalse(summary.copy(id = 4L).isSystem)
    }
}
