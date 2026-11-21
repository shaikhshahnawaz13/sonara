package com.sonara.app.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.SonaraIcons

/**
 * Play/pause. At the mini-player's size it is a plain icon; as the player opens, an accent outline appears
 * around it and the icon grows. [progress] is read only while drawing, so the morph costs no recomposition.
 */
@Composable
internal fun PlayerPlayButton(
    showPause: Boolean,
    buffering: Boolean,
    onClick: () -> Unit,
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .drawBehind {
                val e = progress()
                if (e > 0f) {
                    val stroke = SonaraBorders.emphasis.toPx()
                    drawRect(
                        color = colors.accent.copy(alpha = e),
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke),
                    )
                }
            }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (showPause) SonaraIcons.Pause else SonaraIcons.Play,
            contentDescription = if (showPause) "Pause" else "Play",
            tint = if (buffering) colors.accent else colors.foreground,
            modifier = Modifier
                .size(SonaraSizes.iconStandard)
                .graphicsLayer {
                    val scale = 1f + PLAY_ICON_GROWTH * progress()
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

/**
 * Seek bar: tap to jump, drag to scrub. While dragging, [dragFraction] holds the scrub position and playback
 * keeps going; the seek is sent once when the finger lifts. [position] is read only while drawing.
 */
@Composable
internal fun PlayerSeekBar(
    position: State<Long>,
    durationMs: () -> Long,
    enabled: () -> Boolean,
    dragFraction: MutableState<Float?>,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    val seek by rememberUpdatedState(onSeek)
    val duration by rememberUpdatedState(durationMs)
    val active by rememberUpdatedState(enabled)
    Box(
        modifier = modifier
            .semantics {
                contentDescription = "Seek"
                setProgress { target ->
                    val ms = PlayerGeometry.positionForFraction(target, duration())
                    if (ms > 0L || target <= 0f) seek(ms)
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val total = duration()
                    if (active() && total > 0L) {
                        seek(PlayerGeometry.positionForFraction(PlayerGeometry.seekFraction(offset.x, size.width.toFloat()), total))
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (active() && duration() > 0L) {
                            dragFraction.value = PlayerGeometry.seekFraction(offset.x, size.width.toFloat())
                        }
                    },
                    onHorizontalDrag = { change, _ ->
                        if (dragFraction.value != null) {
                            change.consume()
                            dragFraction.value = PlayerGeometry.seekFraction(change.position.x, size.width.toFloat())
                        }
                    },
                    onDragEnd = {
                        dragFraction.value?.let { seek(PlayerGeometry.positionForFraction(it, duration())) }
                        dragFraction.value = null
                    },
                    onDragCancel = { dragFraction.value = null },
                )
            }
            .drawBehind {
                val fraction = dragFraction.value ?: PlayerGeometry.fractionOf(position.value, duration())
                val trackHeight = TRACK_HEIGHT.toPx()
                val top = (size.height - trackHeight) / 2f
                drawRect(colors.borderMuted, Offset(0f, top), Size(size.width, trackHeight))
                drawRect(colors.accent, Offset(0f, top), Size(size.width * fraction, trackHeight))
                val thumbWidth = THUMB_WIDTH.toPx()
                val thumbHeight = THUMB_HEIGHT.toPx()
                val x = (size.width * fraction - thumbWidth / 2f).coerceIn(0f, size.width - thumbWidth)
                drawRect(colors.accent, Offset(x, (size.height - thumbHeight) / 2f), Size(thumbWidth, thumbHeight))
            }
            .fillMaxSize(),
    )
}

private const val PLAY_ICON_GROWTH = 0.5f
private val TRACK_HEIGHT = 4.dp
private val THUMB_WIDTH = 8.dp
private val THUMB_HEIGHT = 20.dp
