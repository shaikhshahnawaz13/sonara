package com.sonara.app.core.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.roundToInt

/** How much the UI moves. Resolved from the user's animation setting and the system animator scale. */
enum class MotionLevel {
    /** Full choreography. */
    Full,

    /** Shorter durations and no parallax. Slides still happen, so state changes stay spatially readable. */
    Reduced,

    /** No travelling: every change snaps to its end state. */
    Off,
}

/**
 * Every motion number in the app lives here. Retuning the feel of SONARA means editing this object
 * and nothing else. Values are level-independent; [SonaraMotion] applies the reduced-motion rules.
 */
object MotionTokens {

    // Durations (ms)
    const val PRESS_MS = 120
    const val SELECTION_COLOR_MS = 160
    const val PUSH_MS = 400
    const val POP_MS = 360

    /** Library category change: content slide and indicator travel. Inside the 250-350 ms target. */
    const val CATEGORY_MS = 300

    /** Overflow menu: the surface grows out of its button over this long, and folds back a little faster. */
    const val MENU_OPEN_MS = 260
    const val MENU_CLOSE_MS = 200

    /** Mini-player to Full Player: one surface grows over this long, and shrinks back a little faster. */
    const val PLAYER_EXPAND_MS = 420
    const val PLAYER_COLLAPSE_MS = 340

    /** Playlist edit mode: checkbox column, bottom bar and header controls share this duration. */
    const val EDIT_MS = 280

    /** Queue panel inside the Full Player. */
    const val QUEUE_PANEL_MS = 300

    /** Tab travel: one page takes this long when starting from rest. */
    const val TAB_TRAVEL_BASE_MS = 380

    /** Each further page adds this much, so a 3-page trip is faster per page but never rushed. */
    const val TAB_TRAVEL_EXTRA_PAGE_MS = 140

    // Easing curves
    /** Fast start, long soft landing. Used by every slide so paired screens share one timeline. */
    val SlideEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /**
     * Tab travel: a soft start, a smooth middle and a slightly longer landing. Slow at both ends so
     * pages never lurch into motion or stop abruptly.
     */
    val TabTravelEasing: Easing = CubicBezierEasing(0.6f, 0f, 0.25f, 1f)

    /** Quiet curve for small property changes (colour, press compression). */
    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Player expansion: soft start, long soft landing, no overshoot. Paired with the tab-travel feel. */
    val PlayerExpandEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

    /** Player collapse: the mirror of expansion. */
    val PlayerCollapseEasing: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** Menu opening: quick departure from the button, soft landing at full size. No overshoot. */
    val MenuOpenEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Menu closing: the mirror of opening, so the fold-back reads as the same movement reversed. */
    val MenuCloseEasing: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    // Springs. A damping ratio of 1 is critically damped: it settles without any overshoot.
    const val SPRING_DAMPING = 1f
    const val PRESS_RELEASE_STIFFNESS = 700f

    /** Used when a tab tap interrupts a travelling page strip, so the existing velocity carries over. */
    const val TAB_REDIRECT_STIFFNESS = 250f

    // Distances and scales
    /** How far the outgoing screen drifts, as a fraction of its width, when a screen is pushed over it. */
    const val PUSH_PARALLAX_FRACTION = 0.25f

    /** Compression of a pressed control. */
    const val PRESS_SCALE = 0.92f

    // Reduced-motion rule
    const val REDUCED_DURATION_SCALE = 0.6f
}

/**
 * The motion tokens as seen through the current [MotionLevel]. Obtain it with `SonaraTheme.motion`.
 * Every spec is built here, so no component defines its own timing.
 */
@Immutable
class SonaraMotion(val level: MotionLevel) {

    val isOff: Boolean = level == MotionLevel.Off

    private val durationScale: Float = when (level) {
        MotionLevel.Full -> 1f
        MotionLevel.Reduced -> MotionTokens.REDUCED_DURATION_SCALE
        MotionLevel.Off -> 0f
    }

    /** Parallax drift of the outgoing screen on push and pop. Reduced motion removes it. */
    val parallaxFraction: Float =
        if (level == MotionLevel.Full) MotionTokens.PUSH_PARALLAX_FRACTION else 0f

    fun durationMs(baseMs: Int): Int = (baseMs * durationScale).roundToInt()

    /** Travel of the tab page strip from rest across [pages] pages. */
    fun <T> tabTravelSpec(pages: Int): FiniteAnimationSpec<T> {
        val extraPages = pages.coerceAtLeast(1) - 1
        return timed(
            MotionTokens.TAB_TRAVEL_BASE_MS + extraPages * MotionTokens.TAB_TRAVEL_EXTRA_PAGE_MS,
            MotionTokens.TabTravelEasing,
        )
    }

    /**
     * Travel after a tab tap interrupts a strip that is already moving. A critically damped spring
     * honours the current velocity, so reversing direction decelerates and turns instead of stopping dead.
     */
    fun <T> tabRedirectSpec(): FiniteAnimationSpec<T> =
        if (isOff) {
            snap<T>()
        } else {
            spring<T>(
                dampingRatio = MotionTokens.SPRING_DAMPING,
                stiffness = MotionTokens.TAB_REDIRECT_STIFFNESS / (durationScale * durationScale),
            )
        }

    fun <T> pushSpec(): FiniteAnimationSpec<T> = timed(MotionTokens.PUSH_MS, MotionTokens.SlideEasing)

    fun <T> popSpec(): FiniteAnimationSpec<T> = timed(MotionTokens.POP_MS, MotionTokens.SlideEasing)

    /**
     * Library category slide and its indicator. It uses the tab-travel curve so it moves like the
     * bottom navigation, with no overshoot. Reduced motion shortens it; off snaps.
     */
    fun <T> categorySpec(): FiniteAnimationSpec<T> = timed(MotionTokens.CATEGORY_MS, MotionTokens.TabTravelEasing)

    /** Progress 0 to 1 of an overflow menu opening out of its button. Off snaps. */
    fun menuOpenSpec(): FiniteAnimationSpec<Float> = timed(MotionTokens.MENU_OPEN_MS, MotionTokens.MenuOpenEasing)

    /** Progress 1 to 0 of an overflow menu folding back into its button. Off snaps. */
    fun menuCloseSpec(): FiniteAnimationSpec<Float> = timed(MotionTokens.MENU_CLOSE_MS, MotionTokens.MenuCloseEasing)

    /** Progress 0 to 1 of the Full Player opening out of the mini-player. Off snaps. */
    fun playerExpandSpec(): FiniteAnimationSpec<Float> = timed(MotionTokens.PLAYER_EXPAND_MS, MotionTokens.PlayerExpandEasing)

    /** Progress 1 to 0 of the Full Player folding back into the mini-player. Off snaps. */
    fun playerCollapseSpec(): FiniteAnimationSpec<Float> = timed(MotionTokens.PLAYER_COLLAPSE_MS, MotionTokens.PlayerCollapseEasing)

    /** Edit-mode transition of a playlist: checkbox column, bottom bar, header controls. */
    fun <T> editSpec(): FiniteAnimationSpec<T> = timed(MotionTokens.EDIT_MS, MotionTokens.StandardEasing)

    /** The queue panel sliding over the Full Player. */
    fun <T> queuePanelSpec(): FiniteAnimationSpec<T> = timed(MotionTokens.QUEUE_PANEL_MS, MotionTokens.StandardEasing)

    fun <T> selectionColorSpec(): FiniteAnimationSpec<T> =
        timed(MotionTokens.SELECTION_COLOR_MS, MotionTokens.StandardEasing)

    /** Press compression and release. Pressing is timed, release is a non-bouncing spring. */
    fun <T> pressSpec(pressed: Boolean): FiniteAnimationSpec<T> = when {
        isOff -> snap<T>()
        pressed || level == MotionLevel.Reduced ->
            tween<T>(durationMs(MotionTokens.PRESS_MS), easing = MotionTokens.StandardEasing)
        else -> spring<T>(
            dampingRatio = MotionTokens.SPRING_DAMPING,
            stiffness = MotionTokens.PRESS_RELEASE_STIFFNESS,
        )
    }

    private fun <T> timed(baseMs: Int, easing: Easing): FiniteAnimationSpec<T> =
        if (isOff) snap<T>() else tween<T>(durationMs(baseMs), easing = easing)
}

val LocalSonaraMotion = staticCompositionLocalOf { SonaraMotion(MotionLevel.Full) }
