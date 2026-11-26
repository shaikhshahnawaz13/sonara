package com.sonara.app.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonara.app.R
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

private val LogoHeight = 14.dp

/** The official SONARA mountain mark followed by the typographic wordmark. */
@Composable
fun SonaraWordmark(modifier: Modifier = Modifier) {
    val colors = SonaraTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_logo_mark),
            contentDescription = null,
            modifier = Modifier.height(LogoHeight),
            colorFilter = ColorFilter.tint(colors.accent),
        )
        Spacer(modifier = Modifier.width(SonaraSpacing.s8))
        Text(
            text = "SONARA",
            style = SonaraTheme.typography.section.copy(
                fontWeight = FontWeight.Bold,
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
