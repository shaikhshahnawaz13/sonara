package com.sonara.app.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.sonara.app.core.animation.MotionTokens
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraTheme

/**
 * Icon-only action with a 48dp touch target and a brief press compression.
 * [contentDescription] is mandatory because icon-only controls must be labelled for screen readers.
 */
@Composable
fun SonaraIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Overrides the default tint, e.g. to show a toggle as on. */
    tint: Color? = null,
    /**
     * For controls whose icon or tint shows state (play/pause, shuffle, repeat): the tint changes over the
     * selection duration and a changed icon compresses and settles. Left off for ordinary buttons so lists
     * of them carry no extra state.
     */
    animateChange: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = SonaraTheme.motion
    val scale by animateFloatAsState(
        targetValue = if (pressed) MotionTokens.PRESS_SCALE else 1f,
        animationSpec = motion.pressSpec<Float>(pressed),
        label = "iconButtonPress",
    )
    val colors = SonaraTheme.colors

    Box(
        modifier = modifier
            .size(SonaraSizes.minTouchTarget)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val target = tint ?: if (enabled) colors.foreground else colors.disabledContent
        val shown = if (animateChange) {
            animateColorAsState(target, motion.selectionColorSpec(), label = "iconButtonTint").value
        } else {
            target
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = shown,
            modifier = Modifier
                .size(SonaraSizes.iconStandard)
                .then(if (animateChange) Modifier.iconStateChange(icon) else Modifier),
        )
    }
}

@Preview
@Composable
private fun SonaraIconButtonPreview() {
    SonaraTheme {
        SonaraIconButton(icon = SonaraIcons.Search, contentDescription = "Search", onClick = {})
    }
}
