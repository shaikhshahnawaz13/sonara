package com.sonara.app.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme

/**
 * Basic persistent playback summary: artwork, title, artist, play/pause and next. It reflects the real state
 * of the playback service and owns none of it. M5 replaces it with the full mini-player and its
 * expansion into the Full Player, so it deliberately has no expand behaviour yet.
 */
@Composable
fun SonaraMiniPlayer(
    trackId: Long?,
    title: String,
    artist: String,
    isPlaying: Boolean,
    canSkipNext: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    Column(modifier = modifier.fillMaxWidth().background(colors.surfaceRaised)) {
        Box(modifier = Modifier.fillMaxWidth().height(SonaraBorders.standard).background(colors.border))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = SonaraSpacing.s12, end = SonaraSpacing.s4, top = SonaraSpacing.s4, bottom = SonaraSpacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
        ) {
            SonaraArtwork(trackId = trackId, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = SonaraTheme.typography.trackTitle, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = artist, style = SonaraTheme.typography.metadata, color = colors.foregroundMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SonaraIconButton(
                icon = if (isPlaying) SonaraIcons.Pause else SonaraIcons.Play,
                contentDescription = if (isPlaying) "Pause" else "Play",
                onClick = onTogglePlayPause,
            )
            SonaraIconButton(
                icon = SonaraIcons.SkipNext,
                contentDescription = "Next track",
                onClick = onSkipNext,
                enabled = canSkipNext,
            )
        }
    }
}
