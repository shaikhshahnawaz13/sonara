package com.sonara.app.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.sonara.app.core.animation.MotionLevel
import com.sonara.app.core.animation.SonaraMotion
import org.junit.Assert.assertEquals
import org.junit.Test

class NavTransitionKindTest {

    private fun kind(from: String?, to: String?, backward: Boolean = false) =
        classifyNavTransition(from, to, backward)

    @Test
    fun movingToAHigherTabSlidesForward() {
        assertEquals(NavTransitionKind.Tab(1), kind("home", "library"))
        assertEquals(NavTransitionKind.Tab(1), kind("home", "folders"))
        assertEquals(NavTransitionKind.Tab(1), kind("playlists", "folders"))
    }

    @Test
    fun movingToALowerTabSlidesBackward() {
        assertEquals(NavTransitionKind.Tab(-1), kind("folders", "home"))
        assertEquals(NavTransitionKind.Tab(-1), kind("library", "home"))
    }

    @Test
    fun tabDirectionIgnoresTheBackwardFlag() {
        // System back from Library to Home arrives as a pop but must still slide by tab index.
        assertEquals(NavTransitionKind.Tab(-1), kind("library", "home", backward = true))
        assertEquals(NavTransitionKind.Tab(1), kind("home", "library", backward = true))
    }

    @Test
    fun stayingOnTheSameTabHasNoTransition() {
        assertEquals(NavTransitionKind.None, kind("library", "library"))
    }

    @Test
    fun openingSearchOrSettingsIsAPush() {
        assertEquals(NavTransitionKind.Push, kind("home", "search"))
        assertEquals(NavTransitionKind.Push, kind("library", "search"))
        assertEquals(NavTransitionKind.Push, kind("home", "settings"))
    }

    @Test
    fun leavingSearchOrSettingsIsAPop() {
        assertEquals(NavTransitionKind.Pop, kind("search", "library", backward = true))
        assertEquals(NavTransitionKind.Pop, kind("settings", "home", backward = true))
    }

    @Test
    fun routesWithArgumentsCountAsDeeperScreens() {
        // Future detail routes will not match any enum entry; they must still push and pop.
        assertEquals(NavTransitionKind.Push, kind("library", "album/{albumId}"))
        assertEquals(NavTransitionKind.Pop, kind("album/{albumId}", "library", backward = true))
        assertEquals(NavTransitionKind.Push, kind("album/{albumId}", "album/{albumId}"))
        assertEquals(NavTransitionKind.Pop, kind("album/{albumId}", "album/{albumId}", backward = true))
    }

    @Test
    fun everyPrimaryPairGetsTheDirectionOfItsIndices() {
        val tabs = SonaraDestination.primary
        tabs.forEachIndexed { fromIndex, from ->
            tabs.forEachIndexed { toIndex, to ->
                val expected = when {
                    fromIndex == toIndex -> NavTransitionKind.None
                    toIndex > fromIndex -> NavTransitionKind.Tab(1)
                    else -> NavTransitionKind.Tab(-1)
                }
                assertEquals("${from.route} -> ${to.route}", expected, kind(from.route, to.route))
            }
        }
    }

    @Test
    fun offMotionNeverMoves() {
        val off = SonaraMotion(MotionLevel.Off)
        listOf(
            NavTransitionKind.None,
            NavTransitionKind.Tab(1),
            NavTransitionKind.Tab(-1),
            NavTransitionKind.Push,
            NavTransitionKind.Pop,
        ).forEach {
            assertEquals(EnterTransition.None, off.enterFor(it))
            assertEquals(ExitTransition.None, off.exitFor(it))
        }
    }

    @Test
    fun noTransitionKindProducesNoMovement() {
        val full = SonaraMotion(MotionLevel.Full)
        assertEquals(EnterTransition.None, full.enterFor(NavTransitionKind.None))
        assertEquals(ExitTransition.None, full.exitFor(NavTransitionKind.None))
    }
}
