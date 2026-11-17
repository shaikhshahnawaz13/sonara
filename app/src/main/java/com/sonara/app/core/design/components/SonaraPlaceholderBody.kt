package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/**
 * Honest placeholder for a destination whose feature has not been built yet.
 * It deliberately contains no buttons, fake data or sample content: it only states which
 * milestone delivers the screen and what it will show.
 */
@Composable
fun SonaraPlaceholderBody(
    milestone: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    Column(modifier = modifier.fillMaxSize().padding(SonaraSpacing.s16)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(SonaraBorders.standard, colors.borderMuted, SonaraTheme.shapes.small)
                .padding(SonaraSpacing.s16),
        ) {
            Text(
                text = "NOT BUILT YET · $milestone",
                style = SonaraTheme.typography.microLabel,
                color = colors.accent,
            )
            Text(
                text = summary,
                style = SonaraTheme.typography.body,
                color = colors.foregroundMuted,
                modifier = Modifier.padding(top = SonaraSpacing.s8),
            )
        }
    }
}

@Preview
@Composable
private fun SonaraPlaceholderBodyPreview() {
    SonaraTheme {
        SonaraPlaceholderBody(
            milestone = "M3",
            summary = "This screen will list real songs from the device library.",
        )
    }
}
