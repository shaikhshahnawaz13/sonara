package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/** A bottom-navigation entry. [key] is an opaque identifier handed back to the caller on selection. */
data class SonaraNavItem(val key: String, val label: String)

private val SelectedIndicatorHeight = SonaraSpacing.s4

/**
 * Custom neo-brutalist bottom navigation: a 2dp top rule, flat cells, and an acid-lime bar over the
 * selected cell. It pads itself for the navigation-bar inset so it never sits under gesture navigation.
 */
@Composable
fun SonaraBottomNavigation(
    items: List<SonaraNavItem>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SonaraBorders.emphasis)
                .background(colors.border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SonaraSizes.bottomNavHeight)
                .selectableGroup(),
        ) {
            items.forEach { item ->
                NavCell(
                    label = item.label,
                    selected = item.key == selectedKey,
                    onClick = { onSelect(item.key) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavCell(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = SonaraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(if (pressed) colors.surfaceRaised else Color.Transparent)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(SelectedIndicatorHeight)
                    .background(colors.accent),
            )
        }
        Text(
            text = label.uppercase(),
            style = SonaraTheme.typography.microLabel,
            color = if (selected) colors.accent else colors.foregroundMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = SonaraSpacing.s4),
        )
    }
}

@Preview
@Composable
private fun SonaraBottomNavigationPreview() {
    SonaraTheme {
        SonaraBottomNavigation(
            items = listOf(
                SonaraNavItem("home", "Home"),
                SonaraNavItem("library", "Library"),
                SonaraNavItem("playlists", "Playlists"),
                SonaraNavItem("folders", "Folders"),
            ),
            selectedKey = "library",
            onSelect = {},
        )
    }
}
