package com.sonara.app.core.design.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.sonara.app.core.animation.MotionTokens
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraLayout
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.permissions.AudioAccessState
import com.sonara.app.core.permissions.LocalPermissionActions

/**
 * Shared frame of the state components. V2 draws no box: a short heavy rule in the label's colour, the label, then
 * the explanation and action, set on the page with the standard side margin.
 */
@Composable
private fun StatePanel(
    label: String,
    labelColor: Color,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SonaraLayout.screenMargin, vertical = SonaraLayout.blockGap),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        Box(Modifier.width(SonaraSpacing.s24).height(SonaraBorders.rule).background(labelColor))
        Text(text = label, style = SonaraTheme.typography.section, color = labelColor)
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
        LoadingBar()
    }
}

/**
 * Shows that real work is running: an accent segment travelling along a hairline track. It carries no
 * percentage, because there is none to report. Only Full motion moves it; with Reduced or Off it rests at the
 * start, and it stops with the composable, so it never runs off-screen. The position is read only while drawing.
 */
@Composable
private fun LoadingBar() {
    val colors = SonaraTheme.colors
    val motion = SonaraTheme.motion
    val phase = if (motion.sweepsLoading) {
        rememberInfiniteTransition(label = "loadingBar").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(MotionTokens.LOADING_SWEEP_MS, easing = LinearEasing)),
            label = "loadingSweep",
        )
    } else {
        null
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SonaraBorders.emphasis)
            .background(colors.borderMuted)
            .drawBehind {
                val segment = size.width * LOADING_SEGMENT
                val start = if (phase != null) (size.width + segment) * phase.value - segment else 0f
                val left = start.coerceAtLeast(0f)
                val right = (start + segment).coerceAtMost(size.width)
                if (right > left) drawRect(colors.accent, Offset(left, 0f), Size(right - left, size.height))
            },
    )
}

private const val LOADING_SEGMENT = 0.3f

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
