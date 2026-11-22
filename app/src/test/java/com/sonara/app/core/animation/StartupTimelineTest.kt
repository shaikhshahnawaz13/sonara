package com.sonara.app.core.animation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupTimelineTest {

    private val full = SonaraMotion(MotionLevel.Full)
    private val reduced = SonaraMotion(MotionLevel.Reduced)
    private val off = SonaraMotion(MotionLevel.Off)

    @Test
    fun totalLengthIsInsideTheSpecTargetAndScalesWithLevel() {
        assertTrue(MotionTokens.STARTUP_MS in 1200..2000)
        assertEquals(MotionTokens.STARTUP_MS, full.startupMs)
        assertEquals((MotionTokens.STARTUP_MS * MotionTokens.REDUCED_DURATION_SCALE).toInt(), reduced.startupMs)
        assertEquals(0, off.startupMs)
    }

    @Test
    fun phasesAreOrderedAndStartFromNothing() {
        assertEquals(0f, StartupTimeline.wipe(0f), 1e-4f)
        assertEquals(0f, StartupTimeline.rule(0f), 1e-4f)
        assertEquals(0f, StartupTimeline.exit(0f), 1e-4f)
        assertTrue(StartupTimeline.WIPE_END <= StartupTimeline.RULE_END)
        assertTrue(StartupTimeline.RULE_START < StartupTimeline.WIPE_END)
        assertTrue(StartupTimeline.RULE_END <= StartupTimeline.EXIT_START)
    }

    @Test
    fun wordmarkIsFullyRevealedAndRuleDrawnBeforeThePanelLeaves() {
        assertEquals(1f, StartupTimeline.wipe(StartupTimeline.WIPE_END), 1e-4f)
        assertEquals(1f, StartupTimeline.rule(StartupTimeline.RULE_END), 1e-4f)
        assertEquals(0f, StartupTimeline.exit(StartupTimeline.EXIT_START), 1e-4f)
        assertEquals(1f, StartupTimeline.exit(1f), 1e-4f)
    }

    @Test
    fun everyPhaseOnlyMovesForward() {
        val fns = listOf(StartupTimeline::wipe, StartupTimeline::rule, StartupTimeline::exit)
        fns.forEach { f ->
            var last = 0f
            for (i in 0..200) {
                val v = f(i / 200f)
                assertTrue(v in 0f..1f)
                assertTrue(v >= last - 1e-4f)
                last = v
            }
        }
    }

    @Test
    fun clockAdvancesInRealTimeAndNeverLeavesTheRange() {
        // 750 ms of a 1500 ms reveal is half way.
        assertEquals(0.5f, StartupTimeline.advance(0f, 750_000_000L, 1500, 1f), 1e-4f)
        assertEquals(1f, StartupTimeline.advance(0.9f, 5_000_000_000L, 1500, 1f), 0f)
        assertEquals(0f, StartupTimeline.advance(0f, 0L, 1500, 1f), 0f)
    }

    @Test
    fun skippingPlaysTheRestFaster() {
        val normal = StartupTimeline.advance(0.2f, 100_000_000L, 1500, 1f) - 0.2f
        val skipped = StartupTimeline.advance(0.2f, 100_000_000L, 1500, MotionTokens.STARTUP_SKIP_SPEEDUP) - 0.2f
        assertEquals(MotionTokens.STARTUP_SKIP_SPEEDUP, skipped / normal, 1e-3f)
    }

    @Test
    fun zeroLengthFinishesImmediately() {
        assertEquals(1f, StartupTimeline.advance(0f, 16_000_000L, 0, 1f), 0f)
    }

    @Test
    fun newSpecsFollowTheReducedMotionRules() {
        assertTrue(full.iconStateSpec() is androidx.compose.animation.core.TweenSpec)
        assertTrue(reduced.iconStateSpec() is androidx.compose.animation.core.TweenSpec)
        assertTrue(off.iconStateSpec() is androidx.compose.animation.core.SnapSpec)
        assertTrue(off.artworkChangeSpec() is androidx.compose.animation.core.SnapSpec)
        assertEquals(MotionTokens.ARTWORK_CHANGE_MS, (full.artworkChangeSpec() as androidx.compose.animation.core.TweenSpec).durationMillis)
        assertEquals(
            reduced.durationMs(MotionTokens.ARTWORK_CHANGE_MS),
            (reduced.artworkChangeSpec() as androidx.compose.animation.core.TweenSpec).durationMillis,
        )
    }

    @Test
    fun newDurationsStayInsideSpecTargets() {
        assertTrue(MotionTokens.ICON_STATE_MS in 150..220)
        assertTrue(MotionTokens.ARTWORK_CHANGE_MS in 250..400)
    }

    @Test
    fun onlyFullMotionSweepsTheLoadingBar() {
        assertTrue(full.sweepsLoading)
        assertTrue(!reduced.sweepsLoading)
        assertTrue(!off.sweepsLoading)
    }
}
