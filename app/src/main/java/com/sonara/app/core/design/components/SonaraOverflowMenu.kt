package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraTheme

/** One entry of an overflow menu. */
class SonaraMenuAction(val label: String, val onClick: () -> Unit)

/** The three-dot button and its menu. Renders nothing when there are no actions. */
@Composable
fun SonaraOverflowMenu(
    actions: List<SonaraMenuAction>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val colors = SonaraTheme.colors
    SonaraIconButton(
        icon = Icons.Filled.MoreVert,
        contentDescription = contentDescription,
        onClick = { expanded = true },
        modifier = modifier,
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
            .background(colors.surfaceRaised)
            .border(SonaraBorders.standard, colors.border),
    ) {
        actions.forEach { action ->
            DropdownMenuItem(
                text = { Text(action.label, style = SonaraTheme.typography.microLabel, color = colors.foreground) },
                onClick = {
                    expanded = false
                    action.onClick()
                },
            )
        }
    }
}
