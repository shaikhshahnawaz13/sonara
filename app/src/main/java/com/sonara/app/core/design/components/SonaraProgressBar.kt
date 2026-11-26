package com.sonara.app.core.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonara.app.core.design.SonaraTheme

/**
 * Thin, flat progress line: a quiet track and a coral fill. Square ends, no knob, no animation of its own.
 *
 * [progress] is a lambda, 0..1, and is read only while drawing, so a value that changes every frame (playback
 * position) moves the bar without recomposing anything. Used for read-only progress; a draggable seek control is
 * built on top of the same drawing in the player milestone.
 */
@Composable
fun SonaraProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
) {
    val colors = SonaraTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                drawRect(colors.progressTrack)
                val filled = size.width * progress().coerceIn(0f, 1f)
                if (filled > 0f) drawRect(colors.progress, size = Size(filled, size.height))
            },
    )
}
