package com.sonara.app.feature.playlists

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/** Whether the EDIT transition is running. Deliberately not Compose state (see its use). */
internal class EditPhase {
    @Volatile var transitioning = false
}

/**
 * Row placement animation that snaps while [gate] is true and is the usual short tween otherwise. [gate] is
 * consulted when each placement animation starts, so the choice follows the transition without the list
 * having to recompose to learn about it.
 */
internal class GatedPlacementSpec(
    private val ms: Int,
    private val gate: () -> Boolean,
) : FiniteAnimationSpec<IntOffset> {
    override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<IntOffset, V>): VectorizedFiniteAnimationSpec<V> =
        if (gate()) snap<IntOffset>().vectorize(converter) else tween<IntOffset>(ms).vectorize(converter)
}

