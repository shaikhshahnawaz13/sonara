package com.sonara.app.feature.library

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.common.formatDuration
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.SonaraButton
import com.sonara.app.core.permissions.AudioAccessState
import com.sonara.app.core.permissions.LocalPermissionActions
import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.repository.SyncState
import com.sonara.app.playback.RepeatSetting
import kotlinx.coroutines.launch

/**
 * TEMPORARY (M2 only). Lets real discovery and real playback be exercised before the Library screens (M3)
 * and the player (M5) exist. It shows only data that really exists: the permission state, the scan
 * result, the discovered tracks and the live state of the playback service. It is replaced by the real
 * screens and is not part of the final design.
 */
@Composable
fun PlaybackCheckPanel(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val access by container.audioAccess.state.collectAsState()
    val tracks by container.libraryRepository.tracks.collectAsState(initial = emptyList())
    val trackIds = remember(tracks) { tracks.map { it.id } }
    val playback = container.playbackController
    val playbackState by playback.state.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = SonaraSpacing.s16, vertical = SonaraSpacing.s8),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s8),
    ) {
        item { Header() }
        item { AccessCard(access) }
        if (access == AudioAccessState.Granted) {
            item { LibraryCard() }
            item { NowPlayingCard() }
            items(items = tracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    isCurrent = playbackState.currentTrackId == track.id,
                    onClick = { playback.playTracks(trackIds, trackIds.indexOf(track.id)) },
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Text(
        text = "M2 PLAYBACK CHECK · TEMPORARY",
        style = SonaraTheme.typography.microLabel,
        color = SonaraTheme.colors.accent,
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val colors = SonaraTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(SonaraBorders.standard, colors.borderMuted, SonaraTheme.shapes.small)
            .padding(SonaraSpacing.s12),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s8),
    ) { content() }
}

@Composable
private fun AccessCard(access: AudioAccessState) {
    val actions = LocalPermissionActions.current
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    if (access == AudioAccessState.Granted) return
    val settingsName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "Music and audio" else "Storage"
    Card {
        Text("AUDIO ACCESS NEEDED", style = typography.microLabel, color = colors.accent)
        when (access) {
            AudioAccessState.NotRequested -> {
                Text(
                    "SONARA reads the audio files on this device to build your library. Files never leave the device.",
                    style = typography.body, color = colors.foregroundMuted,
                )
                SonaraButton("ALLOW AUDIO ACCESS", onClick = actions.requestAudio, emphasized = true)
            }
            AudioAccessState.Denied -> {
                Text(
                    "Audio access was denied, so SONARA cannot find your music.",
                    style = typography.body, color = colors.foregroundMuted,
                )
                SonaraButton("ASK AGAIN", onClick = actions.requestAudio, emphasized = true)
            }
            else -> {
                Text(
                    "Audio access is blocked. Open system settings and allow \"$settingsName\" for SONARA.",
                    style = typography.body, color = colors.foregroundMuted,
                )
                SonaraButton("OPEN SETTINGS", onClick = actions.openAppSettings, emphasized = true)
            }
        }
    }
}

@Composable
private fun LibraryCard() {
    val container = LocalAppContainer.current
    val repository = container.libraryRepository
    val count by repository.trackCount.collectAsState(initial = 0)
    val sync by repository.syncState.collectAsState()
    val scope = rememberCoroutineScope()
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography

    val status = when (val s = sync) {
        SyncState.Idle -> "Waiting for the first scan"
        SyncState.Scanning -> "Scanning…"
        is SyncState.Done ->
            if (s.skipped) "Up to date (no changes since last scan)"
            else "Scanned: ${s.added} added, ${s.updated} updated, ${s.removed} removed"
        is SyncState.Failed -> "Scan failed: ${s.message}"
    }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$count TRACKS ON THIS DEVICE", style = typography.microLabel, color = colors.foreground)
                Text(status, style = typography.metadata, color = colors.foregroundMuted)
            }
            SonaraButton(
                label = "RESCAN",
                onClick = { scope.launch { repository.sync(force = true) } },
                enabled = sync != SyncState.Scanning,
            )
        }
    }
}

@Composable
private fun NowPlayingCard() {
    val playback = LocalAppContainer.current.playbackController
    val state by playback.state.collectAsState()
    val position by playback.positionMs.collectAsState()
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    var dragging by remember { mutableStateOf<Float?>(null) }

    Card {
        Text("NOW PLAYING", style = typography.microLabel, color = colors.accent)
        if (!state.connected) {
            Text("Connecting to playback service…", style = typography.body, color = colors.foregroundMuted)
            return@Card
        }
        if (!state.hasMedia) {
            Text("Nothing queued. Tap a track below.", style = typography.body, color = colors.foregroundMuted)
            return@Card
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12)) {
            TrackArtwork(trackId = state.currentTrackId, size = 72.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.title.orEmpty(), style = typography.trackTitle, color = colors.foreground,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(state.artist, state.album).joinToString(" · "),
                    style = typography.metadata, color = colors.foregroundMuted,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                val status = when {
                    state.isBuffering -> "BUFFERING"
                    state.isPlaying -> "PLAYING"
                    else -> "PAUSED"
                }
                Text("$status · QUEUE ${state.queueSize}", style = typography.microLabel, color = colors.foregroundMuted)
            }
        }

        val duration = state.durationMs
        val shown = dragging ?: position.toFloat()
        Slider(
            value = shown.coerceIn(0f, duration.coerceAtLeast(1L).toFloat()),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { playback.seekTo(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            enabled = duration > 0,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.borderMuted,
                disabledThumbColor = colors.foregroundMuted,
                disabledActiveTrackColor = colors.foregroundMuted,
                disabledInactiveTrackColor = colors.borderMuted,
            ),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(shown.toLong()), style = typography.metadata, color = colors.foregroundMuted)
            Text(formatDuration(duration), style = typography.metadata, color = colors.foregroundMuted)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s8)) {
            SonaraButton("PREV", onClick = playback::skipToPrevious, modifier = Modifier.weight(1f))
            SonaraButton(
                if (state.isPlaying) "PAUSE" else "PLAY",
                onClick = playback::togglePlayPause,
                emphasized = true,
                modifier = Modifier.weight(1.4f),
            )
            SonaraButton("NEXT", onClick = playback::skipToNext, enabled = state.hasNext || state.repeat != RepeatSetting.Off, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s8)) {
            SonaraButton(
                if (state.shuffleEnabled) "SHUFFLE ON" else "SHUFFLE OFF",
                onClick = { playback.setShuffle(!state.shuffleEnabled) },
                emphasized = state.shuffleEnabled,
                modifier = Modifier.weight(1f),
            )
            SonaraButton(
                "REPEAT ${state.repeat.name.uppercase()}",
                onClick = playback::cycleRepeat,
                emphasized = state.repeat != RepeatSetting.Off,
                modifier = Modifier.weight(1f),
            )
        }
        state.lastError?.let {
            Text("Playback error: $it", style = typography.metadata, color = colors.danger)
        }
    }
}

@Composable
private fun TrackRow(track: TrackEntity, isCurrent: Boolean, onClick: () -> Unit) {
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = SonaraSpacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        TrackArtwork(trackId = track.id, size = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title, style = typography.trackTitle,
                color = if (isCurrent) colors.accent else colors.foreground,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${track.artist} · ${track.album}", style = typography.metadata, color = colors.foregroundMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text(formatDuration(track.durationMs), style = typography.metadata, color = colors.foregroundMuted)
    }
}

/** Real artwork (embedded or album art) or an honest "NO ART" tile; never a stock image. */
@Composable
private fun TrackArtwork(trackId: Long?, size: Dp) {
    val provider = LocalAppContainer.current.artworkProvider
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, trackId, sizePx) {
        value = null
        if (trackId != null) value = provider.load(trackId, sizePx)?.asImageBitmap()
    }
    val colors = SonaraTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .background(colors.surfaceRaised)
            .border(SonaraBorders.standard, colors.borderMuted, SonaraTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        val art = bitmap
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (size >= 64.dp) {
            Text("NO ART", style = SonaraTheme.typography.microLabel, color = colors.foregroundMuted)
        }
    }
}
