package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/** Temporary typographic wordmark with one acid-lime square. The final logo is designed separately. */
@Composable
fun SonaraWordmark(modifier: Modifier = Modifier) {
    val colors = SonaraTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(SonaraSpacing.s12).background(colors.accent))
        Spacer(modifier = Modifier.width(SonaraSpacing.s8))
        Text(
            text = "SONARA",
            style = SonaraTheme.typography.section.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
            ),
            color = colors.foreground,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun SonaraWordmarkPreview() {
    SonaraTheme { SonaraWordmark() }
}
