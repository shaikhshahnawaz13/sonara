package com.sonara.app.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.sonara.app.core.animation.MotionTokens
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/**
 * Text button in the neo-brutalist style: outlined, or filled lime when [emphasized].
 * Uses the same press compression as [SonaraIconButton], from the central motion tokens.
 */
@Composable
fun SonaraButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = SonaraTheme.motion
    val scale by animateFloatAsState(
        targetValue = if (pressed) MotionTokens.PRESS_SCALE else 1f,
        animationSpec = motion.pressSpec<Float>(pressed),
        label = "buttonPress",
    )
    val colors = SonaraTheme.colors
    val container = if (emphasized && enabled) colors.accent else colors.surface
    val content = when {
        !enabled -> colors.foregroundMuted
        emphasized -> colors.onAccent
        else -> colors.foreground
    }
    val borderColor = if (enabled) colors.border else colors.borderMuted

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = SonaraSizes.minTouchTarget, minWidth = SonaraSizes.minTouchTarget)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(container)
            .border(SonaraBorders.standard, borderColor, SonaraTheme.shapes.small)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = SonaraSpacing.s16),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = SonaraTheme.typography.microLabel, color = content)
    }
}

@Preview
@Composable
private fun SonaraButtonPreview() {
    SonaraTheme {
        SonaraButton(label = "ALLOW ACCESS", onClick = {}, emphasized = true)
    }
}
