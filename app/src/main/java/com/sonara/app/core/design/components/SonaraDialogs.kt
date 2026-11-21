package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/**
 * A flat, bordered dialog in SONARA's style. The platform's own dialog animation is switched off, like every
 * other surface in the app: it appears and disappears without fading.
 */
@Composable
fun SonaraDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SonaraTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setWindowAnimations(-1)
        }
        Column(
            modifier = modifier
                .padding(horizontal = SonaraSpacing.s24)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .background(colors.surface)
                .border(SonaraBorders.emphasis, colors.border, SonaraTheme.shapes.small)
                .padding(SonaraSpacing.s16),
            verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
        ) {
            Text(text = title, style = SonaraTheme.typography.section, color = colors.accent)
            content()
        }
    }
}

/** Cancel on the left, the confirming action on the right. */
@Composable
fun SonaraDialogActions(
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s8, Alignment.End),
    ) {
        SonaraButton(label = "CANCEL", onClick = onCancel)
        SonaraButton(label = confirmLabel, onClick = onConfirm, enabled = confirmEnabled, emphasized = true)
    }
}

/** Asks before something that cannot be undone. [body] says exactly what will happen. */
@Composable
fun SonaraConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    SonaraDialog(title = title, onDismiss = onDismiss) {
        Text(text = body, style = SonaraTheme.typography.body, color = SonaraTheme.colors.foregroundMuted)
        SonaraDialogActions(confirmLabel = confirmLabel, onConfirm = onConfirm, onCancel = onDismiss)
    }
}

/**
 * Asks for a name. [error] is shown under the field and cleared as soon as the person edits the text.
 * Confirming with the keyboard's Done key does the same as the confirm button.
 */
@Composable
fun SonaraNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    error: String?,
    onNameEdited: () -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val focus = remember { FocusRequester() }
    SonaraDialog(title = title, onDismiss = onDismiss) {
        // Inside the dialog's own composition, so the field is attached by the time focus is requested.
        LaunchedEffect(Unit) { focus.requestFocus() }
        SonaraTextField(
            value = name,
            onValueChange = {
                name = it
                onNameEdited()
            },
            label = "Playlist name",
            placeholder = "Playlist name",
            errorText = error,
            onDone = { onConfirm(name) },
            focusRequester = focus,
        )
        SonaraDialogActions(confirmLabel = confirmLabel, onConfirm = { onConfirm(name) }, onCancel = onDismiss)
    }
}
