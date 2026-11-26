package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraTheme

/**
 * The V2 way to separate things: a quiet hairline, not a box. [inset] starts the line after a thumbnail so a row's
 * text lines up with the rule. [strong] is the single deliberate heavy line (2dp ink) used above a section.
 */
@Composable
fun SonaraDivider(
    modifier: Modifier = Modifier,
    inset: Dp = 0.dp,
    strong: Boolean = false,
) {
    val colors = SonaraTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(if (strong) SonaraBorders.rule else SonaraBorders.hairline)
            .background(if (strong) colors.border else colors.divider),
    )
}
