package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sonara.app.core.design.SonaraTheme

/**
 * Every screen and tab page paints its own opaque background. Sliding pages never show what is behind
 * them, and nothing depends on alpha. It is the colour the scaffold already had, so nothing changes visually.
 */
@Composable
fun SonaraScreenSurface(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(SonaraTheme.colors.background)) {
        content()
    }
}
