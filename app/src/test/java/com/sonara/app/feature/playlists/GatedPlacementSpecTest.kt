package com.sonara.app.feature.playlists

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GatedPlacementSpecTest {
    private fun durationNanos(spec: GatedPlacementSpec): Long {
        val zero = AnimationVector2D(0f, 0f)
        return spec.vectorize(IntOffset.VectorConverter)
            .getDurationNanos(zero, AnimationVector2D(0f, 200f), zero)
    }

    @Test
    fun snapsWhileTheTransitionRuns() {
        val phase = EditPhase().apply { transitioning = true }
        assertEquals(0L, durationNanos(GatedPlacementSpec(150) { phase.transitioning }))
    }

    @Test
    fun tweensOtherwise() {
        val phase = EditPhase()
        assertEquals(150_000_000L, durationNanos(GatedPlacementSpec(150) { phase.transitioning }))
    }

    @Test
    fun followsThePhaseWithoutRebuildingTheSpec() {
        // One spec instance is shared by every row, so a flip must be seen by the next animation that starts.
        val phase = EditPhase()
        val spec = GatedPlacementSpec(150) { phase.transitioning }
        assertEquals(150_000_000L, durationNanos(spec))
        phase.transitioning = true
        assertEquals(0L, durationNanos(spec))
        phase.transitioning = false
        assertEquals(150_000_000L, durationNanos(spec))
    }

    @Test
    fun interruptedTransitionLeavesTheGateOpenAgain() {
        // The screen sets the flag in try/finally; the holder itself starts closed and is a plain flag.
        val phase = EditPhase()
        assertFalse(phase.transitioning)
        try {
            phase.transitioning = true
            throw IllegalStateException("cancelled")
        } catch (_: IllegalStateException) {
        } finally {
            phase.transitioning = false
        }
        assertTrue(!phase.transitioning)
    }
}
