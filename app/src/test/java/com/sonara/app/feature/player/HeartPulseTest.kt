package com.sonara.app.feature.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartPulseTest {
    @Test
    fun firstLookNeverPulses() {
        assertFalse(heartShouldPulse(false, null, false, 1L, true))
    }

    @Test
    fun addingOrRemovingTheCurrentTrackPulses() {
        assertTrue(heartShouldPulse(true, 1L, false, 1L, true))
        assertTrue(heartShouldPulse(true, 1L, true, 1L, false))
    }

    @Test
    fun changeOfTrackUpdatesQuietlyEvenIfMembershipDiffers() {
        assertFalse(heartShouldPulse(true, 1L, true, 2L, false))
        assertFalse(heartShouldPulse(true, 1L, false, 2L, true))
    }

    @Test
    fun noChangeNoPulse() {
        assertFalse(heartShouldPulse(true, 1L, true, 1L, true))
        assertFalse(heartShouldPulse(true, null, false, null, false))
    }
}
