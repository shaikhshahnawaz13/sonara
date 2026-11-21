package com.sonara.app.core.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/**
 * Square checkbox. Selected is a solid accent square with a check; unselected is an empty outlined square.
 * [enabled] false is the "already there" look: muted and not tappable by itself (the row decides what a tap does).
 */
@Composable
fun SonaraCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SonaraTheme.colors
    val fill = when {
        !checked -> Color.Transparent
        enabled -> colors.accent
        else -> colors.borderMuted
    }
    val outline = when {
        checked && enabled -> colors.accent
        checked -> colors.borderMuted
        enabled -> colors.foreground
        else -> colors.borderMuted
    }
    val mark = if (enabled) colors.onAccent else colors.foregroundMuted
    Canvas(
        modifier = modifier
            .size(CHECKBOX_SIZE)
            .background(fill)
            .border(SonaraBorders.emphasis, outline),
    ) {
        if (checked) {
            val w = size.width
            val h = size.height
            val stroke = 2.5.dp.toPx()
            val a = Offset(w * 0.24f, h * 0.52f)
            val b = Offset(w * 0.43f, h * 0.70f)
            val c = Offset(w * 0.77f, h * 0.31f)
            drawLine(mark, a, b, strokeWidth = stroke, cap = StrokeCap.Square)
            drawLine(mark, b, c, strokeWidth = stroke, cap = StrokeCap.Square)
        }
    }
}

/** Text-only action for bars: accent label, 48dp tall, muted when disabled. */
@Composable
fun SonaraTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SonaraTheme.colors
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = SonaraSizes.minTouchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = SonaraSpacing.s8),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = SonaraTheme.typography.microLabel,
            color = if (enabled) colors.accent else colors.foregroundMuted,
            maxLines = 1,
        )
    }
}

private val CHECKBOX_SIZE = 22.dp
