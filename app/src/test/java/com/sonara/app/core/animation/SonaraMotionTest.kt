package com.sonara.app.core.animation

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SonaraMotionTest {

    private val full = SonaraMotion(MotionLevel.Full)
    private val reduced = SonaraMotion(MotionLevel.Reduced)
    private val off = SonaraMotion(MotionLevel.Off)

    @Test
    fun durationsStayInsideSpecTargets() {
        // Spec section 7: button press 100-160ms, main navigation 350-450ms. Tabs are deliberately shorter.
        assertTrue(MotionTokens.PRESS_MS in 100..160)
        assertTrue(MotionTokens.PUSH_MS in 350..450)
        assertTrue(MotionTokens.POP_MS in 300..450)
        assertTrue(MotionTokens.TAB_MS < MotionTokens.PUSH_MS)
    }

    @Test
    fun parallaxIsAQuarterOfTheWidth() {
        assertEquals(0.25f, MotionTokens.PUSH_PARALLAX_FRACTION, 0f)
        assertEquals(0.25f, full.parallaxFraction, 0f)
    }

    @Test
    fun reducedMotionHasNoParallaxAndOffHasNone() {
        assertEquals(0f, reduced.parallaxFraction, 0f)
        assertEquals(0f, off.parallaxFraction, 0f)
    }

    @Test
    fun durationScalingFollowsLevel() {
        assertEquals(300, full.durationMs(300))
        assertEquals(180, reduced.durationMs(300))
        assertEquals(0, off.durationMs(300))
    }

    @Test
    fun easingCurvesStartAtZeroEndAtOneAndNeverGoBackwards() {
        listOf(MotionTokens.SlideEasing, MotionTokens.StandardEasing).forEach { easing ->
            assertEquals(0f, easing.transform(0f), 1e-4f)
            assertEquals(1f, easing.transform(1f), 1e-4f)
            var previous = 0f
            for (step in 1..50) {
                val value = easing.transform(step / 50f)
                assertTrue("curve went backwards at step $step", value >= previous - 1e-4f)
                previous = value
            }
        }
    }

    @Test
    fun fullMotionNavigationSpecsAreTimedTweens() {
        assertEquals(MotionTokens.TAB_MS, (full.tabSpec<Float>() as TweenSpec<*>).durationMillis)
        assertEquals(MotionTokens.PUSH_MS, (full.pushSpec<Float>() as TweenSpec<*>).durationMillis)
        assertEquals(MotionTokens.POP_MS, (full.popSpec<Float>() as TweenSpec<*>).durationMillis)
    }

    @Test
    fun reducedMotionShortensNavigationSpecs() {
        assertEquals(180, (reduced.tabSpec<Float>() as TweenSpec<*>).durationMillis)
        assertTrue((reduced.pushSpec<Float>() as TweenSpec<*>).durationMillis < MotionTokens.PUSH_MS)
    }

    @Test
    fun offSnapsEverySpec() {
        assertTrue(off.tabSpec<Float>() is SnapSpec<*>)
        assertTrue(off.pushSpec<Float>() is SnapSpec<*>)
        assertTrue(off.popSpec<Float>() is SnapSpec<*>)
        assertTrue(off.selectionColorSpec<Float>() is SnapSpec<*>)
        assertTrue(off.indicatorSpec<Float>() is SnapSpec<*>)
        assertTrue(off.pressSpec<Float>(pressed = true) is SnapSpec<*>)
        assertTrue(off.pressSpec<Float>(pressed = false) is SnapSpec<*>)
    }

    @Test
    fun springsNeverOvershoot() {
        val indicator = full.indicatorSpec<Float>() as SpringSpec<*>
        val release = full.pressSpec<Float>(pressed = false) as SpringSpec<*>
        assertEquals(1f, indicator.dampingRatio, 0f)
        assertEquals(1f, release.dampingRatio, 0f)
        assertEquals(MotionTokens.INDICATOR_STIFFNESS, indicator.stiffness, 0f)
        assertEquals(MotionTokens.PRESS_RELEASE_STIFFNESS, release.stiffness, 0f)
    }

    @Test
    fun pressCompressesWithATweenAndReleasesWithASpring() {
        assertTrue(full.pressSpec<Float>(pressed = true) is TweenSpec<*>)
        assertTrue(full.pressSpec<Float>(pressed = false) is SpringSpec<*>)
    }

    @Test
    fun reducedMotionUsesTweensInsteadOfSprings() {
        assertTrue(reduced.indicatorSpec<Float>() is TweenSpec<*>)
        assertTrue(reduced.pressSpec<Float>(pressed = false) is TweenSpec<*>)
    }

    @Test
    fun pressScaleIsSubtle() {
        assertTrue(MotionTokens.PRESS_SCALE in 0.85f..0.98f)
    }
}
