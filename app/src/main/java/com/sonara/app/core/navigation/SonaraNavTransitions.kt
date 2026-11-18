package com.sonara.app.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.sonara.app.core.animation.SonaraMotion
import kotlin.math.roundToInt

/**
 * The kind of movement between two destinations. Kept free of Compose types so it can be unit tested.
 *
 * - [Tab]: both ends are bottom-navigation tabs. Screens travel side by side, in lockstep, in the
 *   direction of the tab index (+1 = towards a higher index).
 * - [Push]: a deeper screen opens over the current one.
 * - [Pop]: the deeper screen closes and the one beneath it returns.
 */
internal sealed interface NavTransitionKind {
    data object None : NavTransitionKind
    data class Tab(val direction: Int) : NavTransitionKind
    data object Push : NavTransitionKind
    data object Pop : NavTransitionKind
}

/**
 * Decides which choreography applies. Routes that are not bottom-navigation tabs (Search, Settings and
 * the detail routes added later, which carry arguments) all count as deeper screens.
 *
 * [backward] is true when the NavHost asks for a pop transition. Tab-to-tab moves ignore it, because
 * going back from Library to Home must still slide by tab index.
 */
internal fun classifyNavTransition(
    initialRoute: String?,
    targetRoute: String?,
    backward: Boolean,
): NavTransitionKind {
    val from = SonaraDestination.primary.indexOfFirst { it.route == initialRoute }
    val to = SonaraDestination.primary.indexOfFirst { it.route == targetRoute }
    if (from >= 0 && to >= 0) {
        return if (from == to) NavTransitionKind.None else NavTransitionKind.Tab(direction = if (to > from) 1 else -1)
    }
    return if (backward) NavTransitionKind.Pop else NavTransitionKind.Push
}

/*
 * Why tabs move in lockstep: screens are opaque and alpha is not allowed. If a tab slid in over its
 * neighbour by only part of the width, it would appear to cut in. Both tab screens therefore travel one
 * full width on the same timeline and stay edge to edge, so there is no overlap and no z-order dependence.
 *
 * Pushes layer instead: the new screen covers the old one, which drifts back by a quarter of its width.
 */

internal fun SonaraMotion.enterFor(kind: NavTransitionKind): EnterTransition {
    if (isOff) return EnterTransition.None
    return when (kind) {
        NavTransitionKind.None -> EnterTransition.None
        is NavTransitionKind.Tab ->
            slideInHorizontally(animationSpec = tabSpec()) { fullWidth -> fullWidth * kind.direction }
        NavTransitionKind.Push ->
            slideInHorizontally(animationSpec = pushSpec()) { fullWidth -> fullWidth }
        NavTransitionKind.Pop ->
            slideInHorizontally(animationSpec = popSpec()) { fullWidth ->
                -(fullWidth * parallaxFraction).roundToInt()
            }
    }
}

internal fun SonaraMotion.exitFor(kind: NavTransitionKind): ExitTransition {
    if (isOff) return ExitTransition.None
    return when (kind) {
        NavTransitionKind.None -> ExitTransition.None
        is NavTransitionKind.Tab ->
            slideOutHorizontally(animationSpec = tabSpec()) { fullWidth -> -fullWidth * kind.direction }
        NavTransitionKind.Push ->
            slideOutHorizontally(animationSpec = pushSpec()) { fullWidth ->
                -(fullWidth * parallaxFraction).roundToInt()
            }
        NavTransitionKind.Pop ->
            slideOutHorizontally(animationSpec = popSpec()) { fullWidth -> fullWidth }
    }
}
