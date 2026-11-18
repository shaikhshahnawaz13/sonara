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
        // Spec section 7: button press 100-160ms, main navigation 350-450ms.
        assertTrue(MotionTokens.PRESS_MS in 100..160)
        assertTrue(MotionTokens.PUSH_MS in 350..450)
        assertTrue(MotionTokens.POP_MS in 300..450)
        assertTrue(MotionTokens.TAB_TRAVEL_BASE_MS in 300..450)
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
    fun allEasingCurvesStartAtZeroEndAtOneAndNeverGoBackwards() {
        listOf(MotionTokens.SlideEasing, MotionTokens.StandardEasing, MotionTokens.TabTravelEasing)
            .forEach { easing ->
                assertEquals(0f, easing.transform(0f), 1e-4f)
                assertEquals(1f, easing.transform(1f), 1e-4f)
                var previous = 0f
                for (step in 1..100) {
                    val value = easing.transform(step / 100f)
                    assertTrue("curve went backwards at step $step", value >= previous - 1e-4f)
                    previous = value
                }
            }
    }

    @Test
    fun tabTravelCurveStartsGentlyAndLandsSoftly() {
        val curve = MotionTokens.TabTravelEasing
        // Smooth start: almost no movement in the first tenth of the time.
        assertTrue(curve.transform(0.05f) < 0.01f)
        assertTrue(curve.transform(0.1f) < 0.03f)
        // Real speed in the middle, and nearly settled well before the end.
        assertTrue(curve.transform(0.5f) in 0.5f..0.8f)
        assertTrue(curve.transform(0.9f) > 0.97f)
        // Speed ramps up then down: the middle third covers more ground than either outer third.
        val first = curve.transform(0.33f) - curve.transform(0f)
        val middle = curve.transform(0.66f) - curve.transform(0.33f)
        val last = curve.transform(1f) - curve.transform(0.66f)
        assertTrue(middle > first)
        assertTrue(middle > last)
    }

    @Test
    fun tabTravelTimeGrowsWithDistanceButNotProportionally() {
        fun ms(pages: Int) = (full.tabTravelSpec<Float>(pages) as TweenSpec<*>).durationMillis
        assertEquals(MotionTokens.TAB_TRAVEL_BASE_MS, ms(1))
        assertEquals(MotionTokens.TAB_TRAVEL_BASE_MS + MotionTokens.TAB_TRAVEL_EXTRA_PAGE_MS, ms(2))
        assertEquals(MotionTokens.TAB_TRAVEL_BASE_MS + 2 * MotionTokens.TAB_TRAVEL_EXTRA_PAGE_MS, ms(3))
        assertTrue(ms(3) > ms(2) && ms(2) > ms(1))
        assertTrue(ms(3) < 3 * ms(1))
        // Nonsense distances still produce the one-page time.
        assertEquals(ms(1), ms(0))
        assertEquals(ms(1), ms(-4))
    }

    @Test
    fun tabTravelUsesTheTabCurve() {
        val spec = full.tabTravelSpec<Float>(2) as TweenSpec<*>
        assertTrue(spec.easing === MotionTokens.TabTravelEasing)
    }

    @Test
    fun reducedMotionShortensTabTravel() {
        assertEquals(228, (reduced.tabTravelSpec<Float>(1) as TweenSpec<*>).durationMillis)
        assertEquals(312, (reduced.tabTravelSpec<Float>(2) as TweenSpec<*>).durationMillis)
    }

    @Test
    fun fullMotionPushAndPopAreTimedTweens() {
        assertEquals(MotionTokens.PUSH_MS, (full.pushSpec<Float>() as TweenSpec<*>).durationMillis)
        assertEquals(MotionTokens.POP_MS, (full.popSpec<Float>() as TweenSpec<*>).durationMillis)
        assertTrue((reduced.pushSpec<Float>() as TweenSpec<*>).durationMillis < MotionTokens.PUSH_MS)
    }

    @Test
    fun offSnapsEverySpec() {
        assertTrue(off.tabTravelSpec<Float>(3) is SnapSpec<*>)
        assertTrue(off.tabRedirectSpec<Float>() is SnapSpec<*>)
        assertTrue(off.pushSpec<Float>() is SnapSpec<*>)
        assertTrue(off.popSpec<Float>() is SnapSpec<*>)
        assertTrue(off.selectionColorSpec<Float>() is SnapSpec<*>)
        assertTrue(off.pressSpec<Float>(pressed = true) is SnapSpec<*>)
        assertTrue(off.pressSpec<Float>(pressed = false) is SnapSpec<*>)
    }

    @Test
    fun springsNeverOvershootTheirTarget() {
        val redirect = full.tabRedirectSpec<Float>() as SpringSpec<*>
        val release = full.pressSpec<Float>(pressed = false) as SpringSpec<*>
        assertEquals(1f, redirect.dampingRatio, 0f)
        assertEquals(1f, release.dampingRatio, 0f)
        assertEquals(MotionTokens.TAB_REDIRECT_STIFFNESS, redirect.stiffness, 0f)
        assertEquals(MotionTokens.PRESS_RELEASE_STIFFNESS, release.stiffness, 0f)
    }

    @Test
    fun reducedMotionRedirectIsStifferSoItSettlesSooner() {
        val fullRedirect = full.tabRedirectSpec<Float>() as SpringSpec<*>
        val reducedRedirect = reduced.tabRedirectSpec<Float>() as SpringSpec<*>
        assertEquals(1f, reducedRedirect.dampingRatio, 0f)
        assertTrue(reducedRedirect.stiffness > fullRedirect.stiffness)
    }

    @Test
    fun pressCompressesWithATweenAndReleasesWithASpring() {
        assertTrue(full.pressSpec<Float>(pressed = true) is TweenSpec<*>)
        assertTrue(full.pressSpec<Float>(pressed = false) is SpringSpec<*>)
        assertTrue(reduced.pressSpec<Float>(pressed = false) is TweenSpec<*>)
    }

    @Test
    fun pressScaleIsSubtle() {
        assertTrue(MotionTokens.PRESS_SCALE in 0.85f..0.98f)
    }
}
