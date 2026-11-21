package com.sonara.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PlaylistRulesTest {

    @Test
    fun namesAreTrimmedAndWhitespaceCollapsed() {
        assertEquals("My road trip", PlaylistRules.normalizeName("   My   road\ttrip  "))
    }

    @Test
    fun emptyOrBlankNameIsRejected() {
        assertEquals(PlaylistNameError.Empty, PlaylistRules.validateName("", emptyList()))
        assertEquals(PlaylistNameError.Empty, PlaylistRules.validateName("   \t ", emptyList()))
    }

    @Test
    fun nameLengthLimitIsInclusive() {
        val atLimit = "a".repeat(PlaylistRules.MAX_NAME_LENGTH)
        assertNull(PlaylistRules.validateName(atLimit, emptyList()))
        assertEquals(PlaylistNameError.TooLong, PlaylistRules.validateName(atLimit + "a", emptyList()))
    }

    @Test
    fun duplicateNamesAreRejectedIgnoringCaseAndSpacing() {
        val existing = listOf("Gym", "Chill")
        assertEquals(PlaylistNameError.Duplicate, PlaylistRules.validateName("gym", existing))
        assertEquals(PlaylistNameError.Duplicate, PlaylistRules.validateName("  CHILL ", existing))
        assertNull(PlaylistRules.validateName("Focus", existing))
    }

    @Test
    fun renamingMayKeepOrRecaseTheCurrentNameButNotTakeAnotherOne() {
        val existing = listOf("Gym", "Chill")
        assertNull(PlaylistRules.validateName("Gym", existing, current = "Gym"))
        assertNull(PlaylistRules.validateName("GYM", existing, current = "Gym"))
        assertEquals(PlaylistNameError.Duplicate, PlaylistRules.validateName("Chill", existing, current = "Gym"))
    }

    @Test
    fun appendSkipsTracksAlreadyThereAndRepeatsWithinTheRequest() {
        assertEquals(listOf(3L, 4L), PlaylistRules.planAppend(existing = listOf(1L, 2L), incoming = listOf(2L, 3L, 3L, 4L, 1L)))
    }

    @Test
    fun appendToAnEmptyPlaylistKeepsOrder() {
        assertEquals(listOf(9L, 5L, 7L), PlaylistRules.planAppend(emptyList(), listOf(9L, 5L, 7L)))
    }

    @Test
    fun moveDownPlacesTheItemAtTheTargetIndex() {
        assertEquals(listOf("b", "c", "a", "d"), PlaylistRules.move(listOf("a", "b", "c", "d"), 0, 2))
    }

    @Test
    fun moveUpPlacesTheItemAtTheTargetIndex() {
        assertEquals(listOf("a", "d", "b", "c"), PlaylistRules.move(listOf("a", "b", "c", "d"), 3, 1))
    }

    @Test
    fun moveToTheEnds() {
        assertEquals(listOf("d", "a", "b", "c"), PlaylistRules.move(listOf("a", "b", "c", "d"), 3, 0))
        assertEquals(listOf("b", "c", "d", "a"), PlaylistRules.move(listOf("a", "b", "c", "d"), 0, 3))
    }

    @Test
    fun noOpAndOutOfRangeMovesReturnTheSameList() {
        val list = listOf("a", "b", "c")
        assertSame(list, PlaylistRules.move(list, 1, 1))
        assertSame(list, PlaylistRules.move(list, -1, 1))
        assertSame(list, PlaylistRules.move(list, 0, 3))
    }

    @Test
    fun summaryCountsUnavailableEntriesAsTheDifference() {
        val row = com.sonara.app.data.local.database.PlaylistRow(
            id = 1, name = "x", description = null, createdAt = 0, updatedAt = 0,
            entryCount = 5, trackCount = 3, artworkTrackId = 42, durationMs = 1000,
        )
        val summary = row.toSummary()
        assertEquals(3, summary.trackCount)
        assertEquals(2, summary.unavailableCount)
        assertEquals(42L, summary.artworkTrackId)
    }
}
