package com.sonara.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueRestoreTest {

    private val all = setOf(1L, 2L, 3L, 4L, 5L)

    @Test
    fun intactQueueRestoresExactly() {
        val r = QueueRestore.resolve(listOf(1, 2, 3), all, savedIndex = 1, savedPositionMs = 5000)!!
        assertEquals(listOf(1L, 2L, 3L), r.trackIds)
        assertEquals(1, r.startIndex)
        assertEquals(5000L, r.startPositionMs)
    }

    @Test
    fun deletedTrackBeforeCurrentShiftsTheIndexAndKeepsPosition() {
        val r = QueueRestore.resolve(listOf(1, 99, 3), all, savedIndex = 2, savedPositionMs = 7000)!!
        assertEquals(listOf(1L, 3L), r.trackIds)
        assertEquals(1, r.startIndex)
        assertEquals(7000L, r.startPositionMs)
    }

    @Test
    fun deletedCurrentTrackMovesToItsSuccessorFromTheStart() {
        val r = QueueRestore.resolve(listOf(1, 99, 3), all, savedIndex = 1, savedPositionMs = 7000)!!
        assertEquals(listOf(1L, 3L), r.trackIds)
        assertEquals(1, r.startIndex)
        assertEquals(0L, r.startPositionMs)
    }

    @Test
    fun deletedLastCurrentTrackClampsToTheEnd() {
        val r = QueueRestore.resolve(listOf(1, 2, 99), all, savedIndex = 2, savedPositionMs = 100)!!
        assertEquals(1, r.startIndex)
        assertEquals(0L, r.startPositionMs)
    }

    @Test
    fun nothingLeftOrNothingSavedGivesNull() {
        assertNull(QueueRestore.resolve(listOf(98, 99), all, 0, 0))
        assertNull(QueueRestore.resolve(emptyList(), all, 0, 0))
    }

    @Test
    fun outOfRangeSavedIndexIsClamped() {
        val r = QueueRestore.resolve(listOf(1, 2), all, savedIndex = 50, savedPositionMs = 1)!!
        assertEquals(1, r.startIndex)
        assertEquals(0L, r.startPositionMs)
    }

    @Test
    fun adjustStartIndexCountsSurvivorsBeforeTheStart() {
        assertEquals(1, QueueRestore.adjustStartIndex(listOf(true, false, true), 2))
        assertEquals(0, QueueRestore.adjustStartIndex(listOf(true, true), 0))
        assertEquals(-1, QueueRestore.adjustStartIndex(listOf(true), -1))
        assertEquals(0, QueueRestore.adjustStartIndex(listOf(false, false), 1))
        assertEquals(1, QueueRestore.adjustStartIndex(listOf(true, true), 9))
    }
}
