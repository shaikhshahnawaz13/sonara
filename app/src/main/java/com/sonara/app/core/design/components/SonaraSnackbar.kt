package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import kotlinx.coroutines.delay

/** One short message at a time. Showing a new one replaces the old one. */
@Stable
class SonaraSnackbarState {
    var message: String? by mutableStateOf(null)
        private set
    var token: Int by mutableIntStateOf(0)
        private set

    fun show(text: String) {
        message = text
        token++
    }

    internal fun dismiss(forToken: Int) {
        if (token == forToken) message = null
    }
}

val LocalSnackbar = compositionLocalOf { SonaraSnackbarState() }

/** Appears and disappears without animation: SONARA does not fade. Announced to screen readers. */
@Composable
fun SonaraSnackbarHost(state: SonaraSnackbarState, modifier: Modifier = Modifier) {
    val text = state.message ?: return
    val token = state.token
    LaunchedEffect(token) {
        delay(DISPLAY_MS)
        state.dismiss(token)
    }
    val colors = SonaraTheme.colors
    Box(modifier = modifier.fillMaxWidth().padding(SonaraSpacing.s16)) {
        Text(
            text = text,
            style = SonaraTheme.typography.microLabel,
            color = colors.foreground,
            modifier = Modifier
                .background(colors.surfaceRaised)
                .border(SonaraBorders.standard, colors.border, SonaraTheme.shapes.small)
                .padding(SonaraSpacing.s12)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

private const val DISPLAY_MS = 2_200L
