package com.sonara.app.feature.playlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrefetchSlotTest {
    private var now = 0L
    private val slot = PrefetchSlot<String>(maxAgeMs = 1_000L) { now }

    @Test
    fun valueIsReturnedOnceToItsKey() {
        slot.put(1L, "a")
        assertEquals("a", slot.take(1L))
        assertNull(slot.take(1L))
    }

    @Test
    fun otherKeyNeverGetsTheValue() {
        slot.put(1L, "a")
        assertNull(slot.take(2L))
        // A wrong-key look also drops it, so a stale value cannot surface later for a repeat open.
        assertNull(slot.take(1L))
    }

    @Test
    fun staleValueIsDropped() {
        slot.put(1L, "a")
        now = 1_001L
        assertNull(slot.take(1L))
    }

    @Test
    fun valueAtTheAgeLimitIsStillFresh() {
        slot.put(1L, "a")
        now = 1_000L
        assertEquals("a", slot.take(1L))
    }

    @Test
    fun newPutReplacesTheOldOne_rapidOpensOfDifferentPlaylists() {
        slot.put(1L, "a")
        slot.put(2L, "b")
        assertNull(slot.take(1L))
        slot.put(2L, "b2")
        assertEquals("b2", slot.take(2L))
    }

    @Test
    fun repeatedOpenAndCloseOfOnePlaylist() {
        repeat(5) { i ->
            slot.put(3L, "v$i")
            assertEquals("v$i", slot.take(3L))
            assertNull(slot.take(3L))
        }
    }
}
