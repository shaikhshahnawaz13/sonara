package com.sonara.app.core.design.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.permissions.AudioAccessState
import com.sonara.app.core.permissions.LocalPermissionActions

/** Bordered panel shared by the state components. */
@Composable
private fun StatePanel(
    label: String,
    labelColor: Color,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val colors = SonaraTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SonaraSpacing.s16)
            .background(colors.surface)
            .border(SonaraBorders.standard, colors.borderMuted, SonaraTheme.shapes.small)
            .padding(SonaraSpacing.s16),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s8),
    ) {
        Text(text = label, style = SonaraTheme.typography.microLabel, color = labelColor)
        content()
    }
}

/** Nothing to show, and what the person can do about it. */
@Composable
fun SonaraEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StatePanel(title, SonaraTheme.colors.accent, modifier) {
        Text(text = body, style = SonaraTheme.typography.body, color = SonaraTheme.colors.foregroundMuted)
        if (actionLabel != null && onAction != null) {
            SonaraButton(label = actionLabel, onClick = onAction, emphasized = true)
        }
    }
}

/** Honest loading feedback: a label, never an invented percentage. */
@Composable
fun SonaraLoadingState(
    modifier: Modifier = Modifier,
    label: String = "LOADING",
    detail: String? = null,
) {
    StatePanel(label, SonaraTheme.colors.accent, modifier) {
        if (detail != null) {
            Text(text = detail, style = SonaraTheme.typography.body, color = SonaraTheme.colors.foregroundMuted)
        }
    }
}

/** Something failed; say what and offer a retry. */
@Composable
fun SonaraErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    StatePanel("SOMETHING WENT WRONG", SonaraTheme.colors.danger, modifier) {
        Text(text = message, style = SonaraTheme.typography.body, color = SonaraTheme.colors.foregroundMuted)
        if (onRetry != null) SonaraButton(label = "TRY AGAIN", onClick = onRetry, emphasized = true)
    }
}

/** Explains why audio access is needed and offers the step that fits the current permission state. */
@Composable
fun SonaraPermissionPrompt(
    access: AudioAccessState,
    modifier: Modifier = Modifier,
) {
    if (access == AudioAccessState.Granted) return
    val actions = LocalPermissionActions.current
    val settingsName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "Music and audio" else "Storage"
    val (body, label, action) = when (access) {
        AudioAccessState.NotRequested -> Triple(
            "SONARA reads the audio files on this device to build your library. Files never leave the device.",
            "ALLOW AUDIO ACCESS",
            actions.requestAudio,
        )
        AudioAccessState.Denied -> Triple(
            "Audio access was denied, so SONARA cannot find your music.",
            "ASK AGAIN",
            actions.requestAudio,
        )
        else -> Triple(
            "Audio access is blocked. Open system settings and allow \"$settingsName\" for SONARA.",
            "OPEN SETTINGS",
            actions.openAppSettings,
        )
    }
    SonaraEmptyState(title = "AUDIO ACCESS NEEDED", body = body, actionLabel = label, onAction = action, modifier = modifier)
}
