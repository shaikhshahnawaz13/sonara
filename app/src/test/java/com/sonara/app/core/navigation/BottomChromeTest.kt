package com.sonara.app.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A frame clock that advances 16 ms per frame, so animations can run to the end without a device. */
private class SteppingClock : MonotonicFrameClock {
    private var now = 0L
    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
        now += 16_000_000L
        return onFrame(now)
    }
}

class BottomChromeTest {

    // Typical phone: nav = 2dp rule + 64dp bar = 198px content at 3x, plus a 72px gesture inset.
    private val mini = 170
    private val content = 198
    private val nav = content + 72
    private val inset = nav - content

    private fun h(reserved: Boolean, hasMini: Boolean = true) =
        BottomChromeGeometry.height(reserved, if (hasMini) mini else 0, nav, content)

    @Test
    fun shownAndSettledMatchesTheOldLayout() {
        val height = h(reserved = true)
        assertEquals(mini + nav, height)
        assertEquals(0, BottomChromeGeometry.miniTop(height, mini, nav, content, 1f))
        assertEquals(mini, BottomChromeGeometry.navTop(height, nav, 1f))
    }

    @Test
    fun retractedMatchesTheOldLayoutWithoutTheBar() {
        val height = h(reserved = false)
        assertEquals(mini + inset, height)
        assertEquals(0, BottomChromeGeometry.miniTop(height, mini, nav, content, 0f))
        // The bar is entirely below the screen edge.
        assertEquals(height, BottomChromeGeometry.navTop(height, nav, 0f))
    }

    @Test
    fun noMiniPlayerMeansNoSpaceOnceTheBarIsGone() {
        assertEquals(0, h(reserved = false, hasMini = false))
        assertEquals(nav, h(reserved = true, hasMini = false))
    }

    /** The screen bottom is fixed; what a person sees must not depend on when layout space changes hands. */
    @Test
    fun positionsOnScreenAreIdenticalWhetherOrNotSpaceIsReserved() {
        val screenBottom = 2400
        for (step in 0..20) {
            val p = step / 20f
            val reservedH = h(reserved = true)
            val freeH = h(reserved = false)
            val navA = screenBottom - reservedH + BottomChromeGeometry.navTop(reservedH, nav, p)
            val navB = screenBottom - freeH + BottomChromeGeometry.navTop(freeH, nav, p)
            val miniA = screenBottom - reservedH + BottomChromeGeometry.miniTop(reservedH, mini, nav, content, p)
            val miniB = screenBottom - freeH + BottomChromeGeometry.miniTop(freeH, mini, nav, content, p)
            assertEquals("nav at $p", navA, navB)
            assertEquals("mini at $p", miniA, miniB)
        }
    }

    @Test
    fun motionIsMonotonicAndStaysOnScreenBounds() {
        val screenBottom = 2400
        val height = h(reserved = true)
        var lastNav = Int.MIN_VALUE
        var lastMini = Int.MIN_VALUE
        for (step in 20 downTo 0) {
            val p = step / 20f
            val navY = screenBottom - height + BottomChromeGeometry.navTop(height, nav, p)
            val miniY = screenBottom - height + BottomChromeGeometry.miniTop(height, mini, nav, content, p)
            assertTrue(navY >= lastNav)      // retracting only ever moves down
            assertTrue(miniY >= lastMini)
            assertTrue(navY <= screenBottom) // the bar never starts below the screen when shown
            lastNav = navY
            lastMini = miniY
        }
        assertEquals(screenBottom, lastNav)
    }

    @Test
    fun progressIsClampedSoOvershootCannotMoveAnythingPastItsEnds() {
        val height = h(reserved = true)
        assertEquals(
            BottomChromeGeometry.navTop(height, nav, 1f),
            BottomChromeGeometry.navTop(height, nav, 1.3f),
        )
        assertEquals(
            BottomChromeGeometry.navTop(height, nav, 0f),
            BottomChromeGeometry.navTop(height, nav, -0.2f),
        )
    }

    @Test
    fun spaceIsReleasedAtOnceOnOpenAndTakenBackOnlyWhenFullyEmerged() = runBlocking(SteppingClock()) {
        val state = BottomChromeState(shown = true)
        assertTrue(state.emerged)
        assertEquals(1f, state.progress.value)

        state.retract(tween(durationMillis = 1))
        assertFalse(state.emerged)
        assertEquals(0f, state.progress.value)

        // Returning: the bar is still not reserved until the motion has finished.
        state.emerge(tween(durationMillis = 1))
        assertTrue(state.emerged)
        assertEquals(1f, state.progress.value)
    }

    @Test
    fun startingHiddenNeverShowsTheBar() {
        val state = BottomChromeState(shown = false)
        assertFalse(state.emerged)
        assertEquals(0f, state.progress.value)
    }
}
