package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.sonara.app.core.design.SonaraLayout
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/**
 * Consistent screen header: optional back action, a title slot and contextual actions,
 * closed by a 1dp muted rule. Status-bar insets are applied by the hosting Scaffold.
 */
@Composable
fun SonaraTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = SonaraTheme.colors
    Column(modifier = modifier.fillMaxWidth().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SonaraSizes.topBarMinHeight)
                .padding(
                    start = if (onBack != null) SonaraLayout.actionEndInset else SonaraLayout.screenMargin,
                    end = SonaraLayout.actionEndInset,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                SonaraIconButton(
                    icon = SonaraIcons.Back,
                    contentDescription = "Back",
                    onClick = onBack,
                )
                Spacer(modifier = Modifier.width(SonaraSpacing.s4))
            }
            Box(modifier = Modifier.weight(1f)) { title() }
            actions()
        }
        SonaraDivider()
    }
}

/** Convenience overload for a plain screen title. */
@Composable
fun SonaraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    SonaraTopBar(
        title = {
            Text(
                text = title,
                style = SonaraTheme.typography.screenTitle,
                color = SonaraTheme.colors.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        onBack = onBack,
        actions = actions,
    )
}

@Preview
@Composable
private fun SonaraTopBarPreview() {
    SonaraTheme {
        SonaraTopBar(
            title = { SonaraWordmark() },
            actions = {
                SonaraIconButton(SonaraIcons.Search, "Search", onClick = {})
                SonaraIconButton(SonaraIcons.Settings, "Settings", onClick = {})
            },
        )
    }
}
