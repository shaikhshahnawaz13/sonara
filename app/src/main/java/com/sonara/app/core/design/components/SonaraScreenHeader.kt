package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.sonara.app.core.design.SonaraLayout
import com.sonara.app.core.design.SonaraTheme

/**
 * Header of a top-level screen (Home, Library, Playlists, Folders). Every tab uses it, so the title always sits on
 * the same line at the same margin: a fixed [SonaraLayout.headerHeight], an optional upper-case eyebrow above the
 * title, actions at the end, and one hairline underneath. Search comes first among the actions and the screen's
 * own action (rescan, add, settings) last, so they never swap places between tabs.
 *
 * Pushed screens use [SonaraTopBar]; this one has no back action. Status-bar insets come from the Scaffold.
 */
@Composable
fun SonaraScreenHeader(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth().background(SonaraTheme.colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SonaraLayout.headerHeight)
                .padding(start = SonaraLayout.screenMargin, end = SonaraLayout.actionEndInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) { title() }
            actions()
        }
        SonaraDivider()
    }
}

/** A title with an optional [eyebrow] (a short count or status line) above it. */
@Composable
fun SonaraScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    SonaraScreenHeader(
        title = {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    style = SonaraTheme.typography.eyebrow,
                    color = SonaraTheme.colors.foregroundMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                style = SonaraTheme.typography.screenTitle,
                color = SonaraTheme.colors.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        actions = actions,
    )
}

@Preview
@Composable
private fun SonaraScreenHeaderPreview() {
    SonaraTheme {
        SonaraScreenHeader(
            title = "PLAYLISTS",
            eyebrow = "3 playlists",
            actions = {
                SonaraIconButton(SonaraIcons.Search, "Search", onClick = {})
                SonaraIconButton(SonaraIcons.Add, "New playlist", onClick = {})
            },
        )
    }
}
