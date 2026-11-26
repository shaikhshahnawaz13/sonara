package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/**
 * Bordered single-line text input. The border is the accent while focused and red while [errorText] is shown;
 * the error appears under the field in words, never by colour alone.
 */
@Composable
fun SonaraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
    onDone: () -> Unit = {},
    focusRequester: FocusRequester? = null,
) {
    val colors = SonaraTheme.colors
    var focused by remember { mutableStateOf(false) }
    val textStyle = SonaraTheme.typography.trackTitle.copy(color = colors.foreground)
    val borderColor = when {
        errorText != null -> colors.danger
        focused -> colors.accent
        else -> colors.border
    }
    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = SonaraSizes.minTouchTarget)
                        .background(colors.surface)
                        .border(SonaraBorders.emphasis, borderColor, SonaraTheme.shapes.small)
                        .padding(horizontal = SonaraSpacing.s12, vertical = SonaraSpacing.s12),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(text = placeholder, style = textStyle, color = colors.foregroundMuted)
                    }
                    inner()
                }
            },
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = SonaraTheme.typography.microLabel,
                color = colors.danger,
                modifier = Modifier.padding(top = SonaraSpacing.s4),
            )
        }
    }
}
