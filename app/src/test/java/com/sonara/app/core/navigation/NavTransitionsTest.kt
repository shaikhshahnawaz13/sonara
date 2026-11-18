package com.sonara.app.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.sonara.app.core.animation.MotionLevel
import com.sonara.app.core.animation.SonaraMotion
import org.junit.Assert.assertEquals
import org.junit.Test

class NavTransitionsTest {

    @Test
    fun offMotionNeverMoves() {
        val off = SonaraMotion(MotionLevel.Off)
        assertEquals(EnterTransition.None, off.pushEnter())
        assertEquals(ExitTransition.None, off.pushExit())
        assertEquals(EnterTransition.None, off.popEnter())
        assertEquals(ExitTransition.None, off.popExit())
    }

    @Test
    fun fullAndReducedMotionBuildTransitionsWithoutError() {
        listOf(MotionLevel.Full, MotionLevel.Reduced).forEach { level ->
            val motion = SonaraMotion(level)
            motion.pushEnter()
            motion.pushExit()
            motion.popEnter()
            motion.popExit()
        }
    }
}
