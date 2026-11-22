package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/**
 * Compact playback summary above the bottom navigation: artwork, title, artist, play/pause and next. The
 * artwork-and-text area opens the Full Player. It reflects the real state of the playback service and owns
 * none of it.
 *
 * Its layout is the starting point of the Full Player's expansion (see `PlayerGeometry`), so the measurements
 * here (12dp start, 40dp artwork, 12dp gaps, two 48dp buttons, 4dp end) must stay in step with it.
 * While the player is open, [hidden] makes this one invisible (it keeps its place so nothing shifts) and the
 * expanding surface stands in for it.
 */
@Composable
fun SonaraMiniPlayer(
    trackId: Long?,
    title: String,
    artist: String,
    showPause: Boolean,
    buffering: Boolean,
    canSkipNext: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onExpand: () -> Unit,
    hidden: Boolean,
    onBounds: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    // Have the large artwork in memory before the player is opened, so expanding never waits for a decode.
    val loader = LocalArtworkLoader.current
    LaunchedEffect(loader, trackId) {
        if (trackId != null) loader.load(trackId, PLAYER_ARTWORK_PX)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .onGloballyPositioned {
                val b = it.boundsInWindow()
                onBounds(b.left, b.top, b.right, b.bottom)
            }
            .graphicsLayer { alpha = if (hidden) 0f else 1f },
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(SonaraBorders.standard).background(colors.border))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = SonaraSpacing.s4, bottom = SonaraSpacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !hidden, role = Role.Button, onClickLabel = "Open player", onClick = onExpand)
                    .padding(start = SonaraSpacing.s12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
            ) {
                SonaraArtwork(trackId = trackId, size = 40.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = SonaraTheme.typography.trackTitle, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = artist, style = SonaraTheme.typography.metadata, color = colors.foregroundMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            SonaraIconButton(
                icon = if (showPause) SonaraIcons.Pause else SonaraIcons.Play,
                contentDescription = if (showPause) "Pause" else "Play",
                onClick = onTogglePlayPause,
                enabled = !hidden,
                tint = if (buffering) colors.accent else null,
                animateChange = true,
            )
            SonaraIconButton(
                icon = SonaraIcons.SkipNext,
                contentDescription = "Next track",
                onClick = onSkipNext,
                enabled = canSkipNext && !hidden,
                modifier = Modifier.padding(end = SonaraSpacing.s4),
            )
        }
    }
}
