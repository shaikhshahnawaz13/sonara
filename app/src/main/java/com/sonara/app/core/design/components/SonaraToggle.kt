package com.sonara.app.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraTheme

/**
 * On/off switch in the app's square style: a bordered track with a square thumb. Colours ease between the two
 * states with the shared selection timing; the thumb position and colours are read while drawing, so the
 * animation does not recompose anything.
 */
@Composable
fun SonaraToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SonaraTheme.colors
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = SonaraTheme.motion.selectionColorSpec(),
        label = "toggle",
    )
    Box(
        modifier = modifier
            .size(width = TRACK_WIDTH + 4.dp, height = SonaraSizes.minTouchTarget)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = TRACK_WIDTH, height = TRACK_HEIGHT)
                .drawBehind {
                    val p = progress
                    drawRect(lerp(colors.surfaceRaised, colors.accent, p))
                    val thumb = THUMB.toPx()
                    val pad = THUMB_PAD.toPx()
                    val travel = size.width - thumb - 2 * pad
                    drawRoundRect(
                        color = lerp(colors.foregroundMuted, colors.onAccent, p),
                        topLeft = Offset(pad + travel * p, (size.height - thumb) / 2f),
                        size = Size(thumb, thumb),
                        cornerRadius = CornerRadius.Zero,
                    )
                }
                .border(SonaraBorders.emphasis, if (enabled) colors.border else colors.borderMuted),
        )
    }
}

private val TRACK_WIDTH = 52.dp
private val TRACK_HEIGHT = 28.dp
private val THUMB = 16.dp
private val THUMB_PAD = 6.dp
