package com.sonara.app.feature.playlists

import com.sonara.app.data.model.PlaylistSummary
import com.sonara.app.data.repository.AddResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistsLogicTest {

    private fun summary(tracks: Int, unavailable: Int, durationMs: Long) = PlaylistSummary(
        id = 1, name = "Mix", description = null, trackCount = tracks,
        unavailableCount = unavailable, durationMs = durationMs, artworkTrackId = null,
    )

    @Test
    fun metaShowsCountAndDuration() {
        assertEquals("12 TRACKS · 45 MIN", playlistMeta(summary(12, 0, 45 * 60_000L)))
        assertEquals("1 TRACK · 3 MIN", playlistMeta(summary(1, 0, 3 * 60_000L)))
    }

    @Test
    fun emptyPlaylistShowsNoDuration() {
        assertEquals("0 TRACKS", playlistMeta(summary(0, 0, 0)))
    }

    @Test
    fun metaMentionsUnavailableEntries() {
        assertEquals("2 TRACKS · 6 MIN · 3 UNAVAILABLE", playlistMeta(summary(2, 3, 6 * 60_000L)))
        assertEquals("0 TRACKS · 2 UNAVAILABLE", playlistMeta(summary(0, 2, 0)))
    }

    @Test
    fun addMessagesSayWhatHappened() {
        assertEquals("ADDED 3 TRACKS TO GYM", addResultMessage(AddResult(added = 3, alreadyThere = 0), "Gym"))
        assertEquals("ADDED 1 TRACK TO GYM", addResultMessage(AddResult(added = 1, alreadyThere = 0), "Gym"))
        assertEquals("ADDED 2 TO GYM · 1 ALREADY THERE", addResultMessage(AddResult(added = 2, alreadyThere = 1), "Gym"))
        assertEquals("ALREADY IN GYM", addResultMessage(AddResult(added = 0, alreadyThere = 2), "Gym"))
    }

    private val slots = listOf(
        ItemSlot(index = 0, offset = 0, size = 100),
        ItemSlot(index = 1, offset = 100, size = 100),
        ItemSlot(index = 2, offset = 200, size = 100),
    )

    @Test
    fun staysPutWhileTheCentreIsInsideItsOwnSlot() {
        assertNull(findSwapTarget(draggedIndex = 1, draggedCentre = 150f, slots = slots))
    }

    @Test
    fun swapsDownWhenTheCentreEntersTheNextSlot() {
        assertEquals(2, findSwapTarget(draggedIndex = 1, draggedCentre = 210f, slots = slots))
    }

    @Test
    fun swapsUpWhenTheCentreEntersThePreviousSlot() {
        assertEquals(0, findSwapTarget(draggedIndex = 1, draggedCentre = 90f, slots = slots))
    }

    @Test
    fun aFastDragCanJumpSeveralPositions() {
        assertEquals(2, findSwapTarget(draggedIndex = 0, draggedCentre = 290f, slots = slots))
    }

    @Test
    fun noTargetOutsideTheList() {
        assertNull(findSwapTarget(draggedIndex = 2, draggedCentre = 450f, slots = slots))
        assertNull(findSwapTarget(draggedIndex = 0, draggedCentre = -20f, slots = slots))
    }
}
