package com.sonora.music.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SonoraColors = darkColorScheme(
    primary = Color(0xFFB3A6FF),
    onPrimary = Color(0xFF171329),
    secondary = Color(0xFF75D8D0),
    background = Color(0xFF090A10),
    surface = Color(0xFF12141D),
    surfaceVariant = Color(0xFF1B1E2A),
    onBackground = Color(0xFFF3F1FA),
    onSurface = Color(0xFFF3F1FA),
    onSurfaceVariant = Color(0xFFB4B3C2)
)

@Composable
fun SonoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SonoraColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
