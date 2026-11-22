package com.sonara.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonara.app.core.design.AccentChoice
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.LocalSonaraHaptics
import com.sonara.app.core.design.components.SonaraToggle

/*
 * The building blocks of the Settings screens. One visual language throughout: a short accent rule over each
 * section title, a muted one-line description, rows separated by hairlines, square controls with 1dp borders.
 * Nothing here animates beyond what SonaraToggle and the selection colour already do.
 */

/** A section: accent rule, title, description. The rows follow it. */
@Composable
internal fun SettingsSectionHeading(title: String, description: String, modifier: Modifier = Modifier) {
    val colors = SonaraTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = SonaraSpacing.s16, end = SonaraSpacing.s16, top = SonaraSpacing.s32, bottom = SonaraSpacing.s8),
    ) {
        Box(modifier = Modifier.width(SonaraSpacing.s32).height(SonaraBorders.emphasis).background(colors.accent))
        Spacer(Modifier.height(SonaraSpacing.s12))
        Text(text = title, style = SonaraTheme.typography.section, color = colors.foreground)
        Spacer(Modifier.height(SonaraSpacing.s4))
        Text(text = description, style = SonaraTheme.typography.metadata, color = colors.foregroundMuted)
    }
}

/** The hairline between rows of one section. */
@Composable
internal fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SonaraSpacing.s16)
            .height(SonaraBorders.standard)
            .background(SonaraTheme.colors.borderMuted),
    )
}

@Composable
private fun RowText(title: String, description: String?, enabled: Boolean, modifier: Modifier = Modifier) {
    val colors = SonaraTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s4)) {
        Text(
            text = title,
            style = SonaraTheme.typography.trackTitle,
            color = if (enabled) colors.foreground else colors.foregroundMuted,
        )
        if (description != null) {
            Text(text = description, style = SonaraTheme.typography.metadata, color = colors.foregroundMuted)
        }
    }
}

private val RowPadding = Modifier.padding(horizontal = SonaraSpacing.s16, vertical = SonaraSpacing.s12)

/** A switch row. The whole row is the target. */
@Composable
internal fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalSonaraHaptics.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SonaraSizes.minTouchTarget)
            .toggleable(value = checked, role = Role.Switch) {
                haptics.tick()
                onCheckedChange(it)
            }
            .then(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s16),
    ) {
        RowText(title, description, enabled = true, modifier = Modifier.weight(1f))
        // The row handles the tap; the switch only shows the state.
        SonaraToggle(
            checked = checked,
            onCheckedChange = {
                haptics.tick()
                onCheckedChange(it)
            },
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** A tappable row with an optional value on the right, for actions and sub-screens. */
@Composable
internal fun SettingsActionRow(
    title: String,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    enabled: Boolean = true,
    showsMore: Boolean = false,
) {
    val colors = SonaraTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SonaraSizes.minTouchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        RowText(title, description, enabled, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(
                text = value,
                style = SonaraTheme.typography.metadata,
                color = if (enabled) colors.accent else colors.foregroundMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp),
            )
        }
        if (showsMore) {
            Text(text = "›", style = SonaraTheme.typography.section, color = colors.foregroundMuted)
        }
    }
}

/** A read-only row: a label and a value. */
@Composable
internal fun SettingsInfoRow(title: String, value: String, description: String? = null) {
    val colors = SonaraTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SonaraSizes.minTouchTarget)
            .then(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        RowText(title, description, enabled = true, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = SonaraTheme.typography.metadata,
            color = colors.foreground,
            modifier = Modifier.widthIn(max = 180.dp),
        )
    }
}

/** A row of equally wide choices, one selected. Used for sort order and animation level. */
@Composable
internal fun SettingsChoiceRow(
    title: String,
    description: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    val haptics = LocalSonaraHaptics.current
    Column(
        modifier = modifier.fillMaxWidth().then(RowPadding),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        RowText(title, description, enabled = true)
        Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = SonaraSizes.minTouchTarget)
                        .background(if (selected) colors.accent else colors.surfaceRaised)
                        .border(SonaraBorders.standard, if (selected) colors.accent else colors.border)
                        .selectable(selected = selected, role = Role.RadioButton) {
                            if (!selected) haptics.tick()
                            onSelect(index)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label.uppercase(),
                        style = SonaraTheme.typography.microLabel,
                        color = if (selected) colors.onAccent else colors.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = SonaraSpacing.s4),
                    )
                }
            }
        }
    }
}

/** The accent colours as square swatches, the chosen one marked with a frame and a dot. */
@Composable
internal fun SettingsAccentRow(
    selected: AccentChoice,
    onSelect: (AccentChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    val haptics = LocalSonaraHaptics.current
    Column(
        modifier = modifier.fillMaxWidth().then(RowPadding),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        RowText("Accent colour", "${selected.label}. Used for highlights, buttons and the active tab.", enabled = true)
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s8),
        ) {
            AccentChoice.entries.forEach { choice ->
                val isSelected = choice == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(SonaraSizes.minTouchTarget)
                        .background(choice.color)
                        .border(
                            width = if (isSelected) SonaraBorders.emphasis else SonaraBorders.standard,
                            color = if (isSelected) colors.foreground else colors.borderMuted,
                        )
                        .semantics { contentDescription = choice.label }
                        .selectable(selected = isSelected, role = Role.RadioButton) {
                            if (!isSelected) haptics.tick()
                            onSelect(choice)
                        }
                        .drawBehind {
                            if (isSelected) {
                                val dot = 8.dp.toPx()
                                drawRect(
                                    color = androidx.compose.ui.graphics.Color(choice.onArgb),
                                    topLeft = Offset((size.width - dot) / 2f, (size.height - dot) / 2f),
                                    size = Size(dot, dot),
                                )
                            }
                        },
                )
            }
        }
    }
}

/** A message in a note style: bordered, muted, for explanations that are not controls. */
@Composable
internal fun SettingsNote(text: String, modifier: Modifier = Modifier) {
    val colors = SonaraTheme.colors
    Text(
        text = text,
        style = SonaraTheme.typography.metadata,
        color = colors.foregroundMuted,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SonaraSpacing.s16, vertical = SonaraSpacing.s8),
    )
}
