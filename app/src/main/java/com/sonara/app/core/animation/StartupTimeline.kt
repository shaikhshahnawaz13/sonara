package com.sonara.app.core.animation

import androidx.compose.animation.core.Easing

/**
 * The startup reveal as one timeline over progress 0..1, split into phases so a single clock drives all of it:
 *
 *  - 0.00-0.45  the wordmark is wiped in from the left;
 *  - 0.40-0.65  the acid-lime rule draws under it;
 *  - 0.65-0.75  the finished mark is held;
 *  - 0.75-1.00  the whole panel leaves upward and the app, already laid out beneath, is there.
 *
 * Pure functions so the phases can be tested without a device.
 */
object StartupTimeline {
    const val WIPE_END = 0.45f
    const val RULE_START = 0.40f
    const val RULE_END = 0.65f
    const val EXIT_START = 0.75f

    private fun phase(progress: Float, from: Float, to: Float, easing: Easing): Float {
        val t = ((progress - from) / (to - from)).coerceIn(0f, 1f)
        return easing.transform(t)
    }

    /** 0..1: how much of the wordmark is revealed. */
    fun wipe(progress: Float): Float = phase(progress, 0f, WIPE_END, MotionTokens.SlideEasing)

    /** 0..1: how much of the accent rule is drawn. */
    fun rule(progress: Float): Float = phase(progress, RULE_START, RULE_END, MotionTokens.StandardEasing)

    /** 0..1: how far the panel has left upward. */
    fun exit(progress: Float): Float = phase(progress, EXIT_START, 1f, MotionTokens.TabTravelEasing)

    /** Advances the clock by [elapsedNanos]; [speed] above 1 plays faster (after a skip). Never leaves 0..1. */
    fun advance(progress: Float, elapsedNanos: Long, totalMs: Int, speed: Float): Float {
        if (totalMs <= 0) return 1f
        val step = elapsedNanos / (totalMs * 1_000_000f) * speed
        return (progress + step).coerceIn(0f, 1f)
    }
}
