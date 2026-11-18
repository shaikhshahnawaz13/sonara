package com.sonara.app.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.sonara.app.core.animation.SonaraMotion
import kotlin.math.roundToInt

/*
 * Choreography for the NavHost, which now only moves between the tab pager and deeper screens
 * (Search, Settings and the detail screens added later). Movement between tabs is not a navigation
 * transition at all: it is the page strip in SonaraTabs.kt.
 *
 * A deeper screen slides in from the right over the current one, which drifts back by a quarter of its
 * width. Going back reverses it. Screens are opaque and nothing uses alpha. With motion Off, nothing moves.
 */

internal fun SonaraMotion.pushEnter(): EnterTransition =
    if (isOff) {
        EnterTransition.None
    } else {
        slideInHorizontally(animationSpec = pushSpec()) { fullWidth -> fullWidth }
    }

internal fun SonaraMotion.pushExit(): ExitTransition =
    if (isOff) {
        ExitTransition.None
    } else {
        slideOutHorizontally(animationSpec = pushSpec()) { fullWidth ->
            -(fullWidth * parallaxFraction).roundToInt()
        }
    }

internal fun SonaraMotion.popEnter(): EnterTransition =
    if (isOff) {
        EnterTransition.None
    } else {
        slideInHorizontally(animationSpec = popSpec()) { fullWidth ->
            -(fullWidth * parallaxFraction).roundToInt()
        }
    }

internal fun SonaraMotion.popExit(): ExitTransition =
    if (isOff) {
        ExitTransition.None
    } else {
        slideOutHorizontally(animationSpec = popSpec()) { fullWidth -> fullWidth }
    }
