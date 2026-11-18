package com.sonara.app.app

import com.sonara.app.core.animation.MotionLevel
import com.sonara.app.data.local.preferences.AnimationIntensity
import org.junit.Assert.assertEquals
import org.junit.Test

class MotionLevelResolverTest {

    @Test
    fun settingMapsToLevelWhenSystemAnimationsAreOn() {
        assertEquals(MotionLevel.Full, resolveMotionLevel(AnimationIntensity.Full, 1f))
        assertEquals(MotionLevel.Reduced, resolveMotionLevel(AnimationIntensity.Reduced, 1f))
        assertEquals(MotionLevel.Off, resolveMotionLevel(AnimationIntensity.Off, 1f))
    }

    @Test
    fun systemScaleOfZeroForcesOffWhateverTheSetting() {
        AnimationIntensity.entries.forEach {
            assertEquals(MotionLevel.Off, resolveMotionLevel(it, 0f))
        }
    }

    @Test
    fun negativeSystemScaleAlsoForcesOff() {
        assertEquals(MotionLevel.Off, resolveMotionLevel(AnimationIntensity.Full, -1f))
    }

    @Test
    fun otherSystemScalesDoNotChangeTheLevel() {
        // Compose's own clock follows these; the level only reacts to "off".
        assertEquals(MotionLevel.Full, resolveMotionLevel(AnimationIntensity.Full, 0.5f))
        assertEquals(MotionLevel.Full, resolveMotionLevel(AnimationIntensity.Full, 2f))
        assertEquals(MotionLevel.Reduced, resolveMotionLevel(AnimationIntensity.Reduced, 5f))
    }

    @Test
    fun everySettingIsHandled() {
        AnimationIntensity.entries.forEach { resolveMotionLevel(it, 1f) }
    }
}
