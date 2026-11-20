package com.sonara.app.core.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import com.sonara.app.core.design.SonaraTheme

/** Single-line search input with a placeholder and a clear action. */
@Composable
fun SonaraSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search this device",
    focusRequester: FocusRequester? = null,
) {
    val colors = SonaraTheme.colors
    val textStyle = SonaraTheme.typography.trackTitle.copy(color = colors.foreground)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier.fillMaxWidth().let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = textStyle, color = colors.foregroundMuted)
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    SonaraIconButton(
                        icon = SonaraIcons.Close,
                        contentDescription = "Clear search",
                        onClick = { onValueChange("") },
                    )
                }
            }
        },
    )
}
