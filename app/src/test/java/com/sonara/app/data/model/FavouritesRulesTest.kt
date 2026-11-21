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
}
