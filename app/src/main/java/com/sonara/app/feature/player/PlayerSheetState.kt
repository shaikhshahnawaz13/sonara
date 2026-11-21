package com.sonara.app.feature.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * The one number that describes the mini-player turning into the Full Player: [progress], 0 (a mini-player)
 * to 1 (the full screen). Everything on screen is a function of it, so an interrupted expansion simply
 * continues from wherever it is, in either direction. It knows nothing about playback.
 */
@Stable
class PlayerSheetState(initiallyExpanded: Boolean = false) {

    val progress = Animatable(if (initiallyExpanded) 1f else 0f)

    /** Where the person last asked the player to be. Not necessarily where it is yet. */
    var expanded: Boolean by mutableStateOf(initiallyExpanded)
        private set

    /**
     * True from the first frame of an expansion to the last frame of a collapse. The overlay is composed and the
     * resting mini-player is hidden exactly while this is true, and both change in the same composition, so
     * the surface never has a frame with two mini-players or none.
     */
    var overlayShown: Boolean by mutableStateOf(initiallyExpanded)
        private set

    var queueOpen: Boolean by mutableStateOf(false)

    /** The mini-player's bounds in the window (left, top, right, bottom). Read while laying out; not Compose state. */
    var miniBounds: FloatArray? = null
        private set

    fun updateMiniBounds(left: Float, top: Float, right: Float, bottom: Float) {
        val old = miniBounds
        if (old != null && old[0] == left && old[1] == top && old[2] == right && old[3] == bottom) return
        miniBounds = floatArrayOf(left, top, right, bottom)
    }

    suspend fun expand(spec: FiniteAnimationSpec<Float>) {
        expanded = true
        overlayShown = true
        progress.animateTo(1f, spec)
    }

    /** Collapses. If an expansion interrupts it, this is cancelled before it can hide the overlay. */
    suspend fun collapse(spec: FiniteAnimationSpec<Float>) {
        expanded = false
        queueOpen = false
        progress.animateTo(0f, spec)
        if (!expanded) overlayShown = false
    }

    /** Immediately closed, e.g. when there is nothing left to play. */
    suspend fun closeNow() {
        expanded = false
        queueOpen = false
        progress.snapTo(0f)
        overlayShown = false
    }

    companion object {
        val Saver: Saver<PlayerSheetState, Boolean> = Saver(
            save = { it.expanded },
            restore = { PlayerSheetState(it) },
        )
    }
}

/** A player sheet that survives rotation: an expanded player is still expanded afterwards. */
@Composable
fun rememberPlayerSheetState(): PlayerSheetState =
    rememberSaveable(saver = PlayerSheetState.Saver) { PlayerSheetState() }
