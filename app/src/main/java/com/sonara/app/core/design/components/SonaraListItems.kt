package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonara.app.core.common.formatDuration
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/** Section title with an optional text action on the right. */
@Composable
fun SonaraSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = SonaraTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(top = SonaraSpacing.s16, bottom = SonaraSpacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = SonaraTheme.typography.section,
            color = colors.foreground,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = SonaraSizes.minTouchTarget)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = SonaraSpacing.s8),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = actionLabel, style = SonaraTheme.typography.microLabel, color = colors.accent)
            }
        }
    }
}

/**
 * Song row: artwork (or a track number inside an album), title, subtitle, duration and an overflow menu.
 * The current track is highlighted in the accent colour.
 */
@Composable
fun SonaraTrackRow(
    title: String,
    subtitle: String,
    durationMs: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkTrackId: Long? = null,
    leadingNumber: Int? = null,
    isCurrent: Boolean = false,
    /** Muted title, for an entry whose file is not available. */
    dimmed: Boolean = false,
    menu: List<SonaraMenuAction> = emptyList(),
    /** Extra control after the menu, such as a reorder handle. */
    trailing: @Composable () -> Unit = {},
    /** Null outside selection mode. Otherwise a checkbox leads the row and a selected row is marked. */
    selected: Boolean? = null,
    /** In selection mode: the song is already there and cannot be picked (e.g. already in the playlist). */
    locked: Boolean = false,
    lockedLabel: String = "ADDED",
) {
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    val marked = selected == true && !locked
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(
                if (selected != null) {
                    Modifier
                        .background(if (marked) colors.surfaceRaised else Color.Transparent)
                        .drawBehind {
                            if (marked) drawRect(colors.accent, size = Size(SELECTED_BAR.toPx(), size.height))
                        }
                        .semantics { this.selected = marked }
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = SonaraSpacing.s16, end = SonaraSpacing.s4, top = SonaraSpacing.s4, bottom = SonaraSpacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        if (selected != null) {
            SonaraCheckbox(checked = selected || locked, enabled = !locked)
        }
        if (leadingNumber != null) {
            Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (leadingNumber > 0) leadingNumber.toString() else "–",
                    style = typography.metadata,
                    color = if (isCurrent) colors.accent else colors.foregroundMuted,
                )
            }
        } else {
            SonaraArtwork(trackId = artworkTrackId, size = 48.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.trackTitle,
                color = if (isCurrent) colors.accent else if (dimmed || locked) colors.foregroundMuted else colors.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = typography.metadata,
                color = colors.foregroundMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (locked) {
            Text(text = lockedLabel, style = typography.microLabel, color = colors.foregroundMuted)
        } else if (durationMs != null) {
            Text(text = formatDuration(durationMs), style = typography.metadata, color = colors.foregroundMuted)
        }
        if (menu.isNotEmpty()) {
            SonaraOverflowMenu(actions = menu, contentDescription = "More actions for $title")
        } else {
            Box(modifier = Modifier.width(SonaraSpacing.s12))
        }
        trailing()
    }
}

/** Album tile for grids and rows: square artwork as wide as the cell, then title, artist and track count. */
@Composable
fun SonaraAlbumCard(
    title: String,
    artist: String,
    trackCount: Int,
    artworkTrackId: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    Column(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(SonaraSpacing.s8),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s4),
    ) {
        SonaraArtworkFill(trackId = artworkTrackId)
        Text(text = title, style = typography.trackTitle, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = artist, style = typography.metadata, color = colors.foregroundMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = plural(trackCount, "track").uppercase(), style = typography.microLabel, color = colors.foregroundMuted)
    }
}

/** Plain two-line row, for genres and similar simple lists. */
@Composable
fun SonaraListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SonaraSpacing.s16, vertical = SonaraSpacing.s8),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = SonaraTheme.typography.trackTitle, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = subtitle, style = SonaraTheme.typography.metadata, color = colors.foregroundMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Artist row: artwork of one of their tracks, name and local counts. */
@Composable
fun SonaraArtistRow(
    name: String,
    albumCount: Int,
    trackCount: Int,
    artworkTrackId: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SonaraSpacing.s16, vertical = SonaraSpacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        SonaraArtwork(trackId = artworkTrackId, size = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = typography.trackTitle, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "${plural(albumCount, "album")} · ${plural(trackCount, "track")}",
                style = typography.metadata,
                color = colors.foregroundMuted,
            )
        }
    }
}

/** Playlist row: artwork of its first available track, name, counts and an overflow menu. */
@Composable
fun SonaraPlaylistRow(
    name: String,
    subtitle: String,
    artworkTrackId: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menu: List<SonaraMenuAction> = emptyList(),
) {
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = SonaraSpacing.s16, end = SonaraSpacing.s4, top = SonaraSpacing.s4, bottom = SonaraSpacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        SonaraArtwork(trackId = artworkTrackId, size = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = typography.trackTitle, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, style = typography.metadata, color = colors.foregroundMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (menu.isNotEmpty()) {
            SonaraOverflowMenu(actions = menu, contentDescription = "More actions for $name")
        } else {
            Box(modifier = Modifier.width(SonaraSpacing.s12))
        }
    }
}

/** Folder row: name, location, track count and an optional access status. */
@Composable
fun SonaraFolderRow(
    name: String,
    location: String,
    trackCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: String? = null,
    statusIsProblem: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = SonaraSpacing.s16, end = SonaraSpacing.s4, top = SonaraSpacing.s8, bottom = SonaraSpacing.s8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = typography.trackTitle, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = location, style = typography.metadata, color = colors.foregroundMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (status != null) {
                Text(text = status, style = typography.microLabel, color = if (statusIsProblem) colors.danger else colors.accent)
            }
        }
        Text(text = plural(trackCount, "track"), style = typography.metadata, color = colors.foregroundMuted)
        trailing()
    }
}

/** A bordered tile used for Home's quick access. */
@Composable
fun SonaraTile(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    Column(
        modifier = modifier
            .background(colors.surface)
            .border(SonaraBorders.standard, colors.borderMuted, SonaraTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = SonaraSizes.minTouchTarget)
            .padding(SonaraSpacing.s12),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s4),
    ) {
        Text(text = label, style = SonaraTheme.typography.microLabel, color = colors.accent)
        Text(text = value, style = SonaraTheme.typography.metadata, color = colors.foregroundMuted)
    }
}

fun plural(count: Int, noun: String): String = if (count == 1) "1 $noun" else "$count ${noun}s"

/** Width of the accent bar that marks a selected row. */
private val SELECTED_BAR = 3.dp
