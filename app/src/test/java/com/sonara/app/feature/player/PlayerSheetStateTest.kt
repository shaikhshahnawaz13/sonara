package com.sonara.app.feature.player

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.saveable.SaverScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Frames arrive one coroutine turn apart, 16 ms of animation time each, so no real time passes. */
private class FakeFrameClock : MonotonicFrameClock {
    private var now = 0L
    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
        yield()
        now += 16_000_000L
        return onFrame(now)
    }
}

class PlayerSheetStateTest {

    @Test
    fun startsCollapsedWithNothingOnScreen() {
        val sheet = PlayerSheetState()
        assertFalse(sheet.expanded)
        assertFalse(sheet.overlayShown)
        assertEquals(0f, sheet.progress.value, 0f)
    }

    @Test
    fun startsExpandedWhenRestoredThatWay() {
        val sheet = PlayerSheetState(initiallyExpanded = true)
        assertTrue(sheet.expanded)
        assertTrue(sheet.overlayShown)
        assertEquals(1f, sheet.progress.value, 0f)
    }

    @Test
    fun expandingEndsFullyOpenWithTheOverlayShown() = runBlocking(FakeFrameClock()) {
        val sheet = PlayerSheetState()
        sheet.expand(tween(200))
        assertTrue(sheet.expanded)
        assertTrue(sheet.overlayShown)
        assertEquals(1f, sheet.progress.value, 0f)
    }

    @Test
    fun collapsingEndsClosedAndRemovesTheOverlayOnlyAtTheEnd() = runBlocking(FakeFrameClock()) {
        val sheet = PlayerSheetState(initiallyExpanded = true)
        sheet.queueOpen = true
        sheet.collapse(tween(200))
        assertFalse(sheet.expanded)
        assertFalse(sheet.queueOpen)
        assertFalse(sheet.overlayShown)
        assertEquals(0f, sheet.progress.value, 0f)
    }

    @Test
    fun theOverlayStaysUntilTheCollapseHasFinished() = runBlocking(FakeFrameClock()) {
        val sheet = PlayerSheetState(initiallyExpanded = true)
        val job = launch { sheet.collapse(tween(100_000)) }
        repeat(5) { yield() }
        assertTrue(job.isActive)
        assertTrue("overlay must remain while it is still shrinking", sheet.overlayShown)
        assertFalse(sheet.expanded)
        job.cancel()
    }

    @Test
    fun anExpansionInterruptingACollapseWinsAndKeepsTheOverlay() = runBlocking(FakeFrameClock()) {
        val sheet = PlayerSheetState(initiallyExpanded = true)
        val collapsing = launch { sheet.collapse(tween(100_000)) }
        repeat(5) { yield() }
        assertTrue(collapsing.isActive)

        sheet.expand(snap())
        collapsing.join()

        assertTrue(collapsing.isCancelled)
        assertTrue(sheet.expanded)
        assertTrue(sheet.overlayShown)
        assertEquals(1f, sheet.progress.value, 0f)
    }

    @Test
    fun aCollapseInterruptingAnExpansionWinsAndEndsClosed() = runBlocking(FakeFrameClock()) {
        val sheet = PlayerSheetState()
        val expanding = launch { sheet.expand(tween(100_000)) }
        repeat(5) { yield() }
        assertTrue(expanding.isActive)

        sheet.collapse(snap())
        expanding.join()

        assertFalse(sheet.expanded)
        assertFalse(sheet.overlayShown)
        assertEquals(0f, sheet.progress.value, 0f)
    }

    @Test
    fun repeatedExpandAndCollapseAlwaysEndWhereTheLastRequestSaid() = runBlocking(FakeFrameClock()) {
        val sheet = PlayerSheetState()
        repeat(10) { i ->
            val job = launch { if (i % 2 == 0) sheet.expand(tween(100_000)) else sheet.collapse(tween(100_000)) }
            repeat(3) { yield() }
            if (i == 9) job.cancel()
        }
        sheet.expand(snap())
        assertTrue(sheet.expanded && sheet.overlayShown)
        assertEquals(1f, sheet.progress.value, 0f)
    }

    @Test
    fun closeNowClosesImmediately() = runBlocking(FakeFrameClock()) {
        val sheet = PlayerSheetState(initiallyExpanded = true)
        sheet.queueOpen = true
        sheet.closeNow()
        assertFalse(sheet.expanded)
        assertFalse(sheet.overlayShown)
        assertFalse(sheet.queueOpen)
        assertEquals(0f, sheet.progress.value, 0f)
    }

    @Test
    fun miniBoundsAreKeptAndAnUnchangedReportKeepsTheSameArray() {
        val sheet = PlayerSheetState()
        assertEquals(null, sheet.miniBounds)
        sheet.updateMiniBounds(0f, 100f, 1080f, 271f)
        val first = sheet.miniBounds
        assertNotNull(first)
        sheet.updateMiniBounds(0f, 100f, 1080f, 271f)
        assertTrue(first === sheet.miniBounds)
        sheet.updateMiniBounds(0f, 120f, 1080f, 291f)
        assertEquals(120f, sheet.miniBounds!![1], 0f)
    }

    @Test
    fun theSavedStateIsWhetherThePlayerWasOpen() {
        val scope = SaverScope { true }
        val open = PlayerSheetState(initiallyExpanded = true)
        val saved = with(PlayerSheetState.Saver) { scope.save(open) }
        assertEquals(true, saved)
        val restored = PlayerSheetState.Saver.restore(true)
        assertNotNull(restored)
        assertTrue(restored!!.expanded && restored.overlayShown)
        assertFalse(PlayerSheetState.Saver.restore(false)!!.expanded)
    }
}
