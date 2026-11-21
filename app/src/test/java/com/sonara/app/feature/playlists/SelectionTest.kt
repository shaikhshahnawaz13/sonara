package com.sonara.app.feature.playlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionTest {

    @Test
    fun startsEmpty() {
        assertTrue(Selection.NONE.isEmpty)
        assertEquals(0, Selection.NONE.count)
    }

    @Test
    fun toggleAddsThenRemoves() {
        val one = Selection.NONE.toggle(5)
        assertTrue(5L in one)
        assertEquals(1, one.count)
        val none = one.toggle(5)
        assertFalse(5L in none)
        assertTrue(none.isEmpty)
    }

    @Test
    fun keepsTheOrderItemsWerePicked() {
        val picked = Selection.NONE.toggle(30).toggle(10).toggle(20)
        assertEquals(listOf(30L, 10L, 20L), picked.ids.toList())
    }

    @Test
    fun selectAllAppendsMissingInListOrderAndNeverDuplicates() {
        val s = Selection.NONE.toggle(3).selectAll(listOf(1, 2, 3, 4))
        assertEquals(listOf(3L, 1L, 2L, 4L), s.ids.toList())
        assertEquals(4, s.count)
    }

    @Test
    fun coversAllOnlyWhenEveryCandidateIsSelected() {
        val candidates = listOf(1L, 2L, 3L)
        assertFalse(Selection.NONE.coversAll(candidates))
        assertFalse(Selection.NONE.toggle(1).coversAll(candidates))
        assertTrue(Selection.NONE.selectAll(candidates).coversAll(candidates))
        assertFalse("nothing to cover", Selection.NONE.coversAll(emptyList()))
    }

    @Test
    fun clearEmptiesTheSelection() {
        assertTrue(Selection.NONE.selectAll(listOf(1, 2)).clear().isEmpty)
        assertSame(Selection.NONE, Selection.NONE.clear())
    }

    @Test
    fun retainOnlyDropsIdsThatAreNoLongerValid() {
        val s = Selection.NONE.selectAll(listOf(1, 2, 3))
        val kept = s.retainOnly(listOf(2, 3, 9))
        assertEquals(listOf(2L, 3L), kept.ids.toList())
    }

    @Test
    fun retainOnlyKeepsTheSameInstanceWhenNothingChanges() {
        val s = Selection.NONE.selectAll(listOf(1, 2))
        assertSame(s, s.retainOnly(listOf(1, 2, 3)))
        assertSame(Selection.NONE, Selection.NONE.retainOnly(listOf(1)))
    }

    @Test
    fun selectedSongsMinusThoseAlreadyInThePlaylistAreWhatGetsAdded() {
        // The picker only offers addable ids, so a locked song can never enter the selection by Select all.
        val library = listOf(1L, 2L, 3L, 4L)
        val inPlaylist = setOf(2L, 4L)
        val addable = library.filter { it !in inPlaylist }
        val s = Selection.NONE.selectAll(addable)
        assertEquals(listOf(1L, 3L), s.ids.toList())
        assertTrue(s.coversAll(addable))
    }
}
