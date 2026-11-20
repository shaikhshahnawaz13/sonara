package com.sonara.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.SonaraArtwork
import com.sonara.app.core.design.components.SonaraErrorState
import com.sonara.app.core.design.components.SonaraIconButton
import com.sonara.app.core.design.components.SonaraLoadingState
import com.sonara.app.core.design.components.SonaraPermissionPrompt
import com.sonara.app.core.design.components.SonaraSectionHeader
import com.sonara.app.core.design.components.SonaraTile
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.design.components.SonaraTrackRow
import com.sonara.app.core.design.components.SonaraWordmark
import com.sonara.app.core.design.components.plural
import com.sonara.app.core.navigation.LibraryCategory
import com.sonara.app.core.navigation.LocalSonaraNavigator
import com.sonara.app.core.navigation.SonaraDestination
import com.sonara.app.core.permissions.AudioAccessState
import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.repository.SyncState
import com.sonara.app.feature.common.LibraryEmptyState
import com.sonara.app.feature.common.UiState
import com.sonara.app.feature.common.appViewModel
import com.sonara.app.feature.common.rememberPlaybackActions
import com.sonara.app.feature.common.rememberCurrentTrackId
import com.sonara.app.feature.common.rememberTrackMenu
import com.sonara.app.core.design.components.SonaraIcons

@Composable
fun HomeScreen() {
    val container = LocalAppContainer.current
    val navigator = LocalSonaraNavigator.current
    val viewModel = appViewModel { HomeViewModel(it.libraryRepository, it.folderRepository) }
    val access by container.audioAccess.state.collectAsStateWithLifecycle()
    val sync by viewModel.syncState.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val colors = SonaraTheme.colors

    val trackCount = (content as? UiState.Content<HomeContent>)?.data?.counts?.tracks
    val status = when {
        access != AudioAccessState.Granted -> "AUDIO ACCESS NEEDED"
        sync == SyncState.Scanning -> "SCANNING LIBRARY…"
        sync is SyncState.Failed -> "LAST SCAN FAILED"
        trackCount != null -> "$trackCount TRACKS ON THIS DEVICE"
        else -> "LOADING LIBRARY"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(
            title = {
                Column {
                    SonaraWordmark()
                    Text(status, style = SonaraTheme.typography.microLabel, color = colors.foregroundMuted)
                }
            },
            actions = {
                SonaraIconButton(SonaraIcons.Search, "Search", onClick = navigator::openSearch)
                SonaraIconButton(SonaraIcons.Settings, "Settings", onClick = navigator::openSettings)
            },
        )
        if (access != AudioAccessState.Granted) {
            SonaraPermissionPrompt(access)
            return@Column
        }
        when (val state = content) {
            UiState.Loading -> SonaraLoadingState()
            is UiState.Error -> SonaraErrorState(state.message, onRetry = viewModel::retry)
            is UiState.Content -> HomeContentList(state.data, sync, onRescan = viewModel::rescan)
        }
    }
}

@Composable
private fun HomeContentList(content: HomeContent, sync: SyncState, onRescan: () -> Unit) {
    val navigator = LocalSonaraNavigator.current
    val actions = rememberPlaybackActions()
    val currentTrackId by rememberCurrentTrackId()
    val counts = content.counts
    val addedIds = remember(content.recentlyAdded) { content.recentlyAdded.map { it.id } }
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(horizontal = SonaraSpacing.s16, vertical = SonaraSpacing.s16)) {
                Text("YOUR MUSIC", style = typography.hero, color = colors.foreground)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = SonaraSpacing.s12),
                    horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s8),
                ) {
                    Stat("TRACKS", counts.tracks, Modifier.weight(1f))
                    Stat("ALBUMS", counts.albums, Modifier.weight(1f))
                    Stat("ARTISTS", counts.artists, Modifier.weight(1f))
                    Stat("PLAYLISTS", counts.playlists, Modifier.weight(1f))
                }
            }
        }
        if (counts.tracks == 0) {
            item { LibraryEmptyState(sync = sync, onRescan = onRescan) }
            return@LazyColumn
        }

        item { Section("QUICK ACCESS") }
        item {
            Column(
                modifier = Modifier.padding(horizontal = SonaraSpacing.s16),
                verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s8),
            ) {
                val tiles = listOf(
                    TileSpec("SONGS", plural(counts.tracks, "track")) { navigator.openLibrary(LibraryCategory.Songs) },
                    TileSpec("ALBUMS", plural(counts.albums, "album")) { navigator.openLibrary(LibraryCategory.Albums) },
                    TileSpec("ARTISTS", plural(counts.artists, "artist")) { navigator.openLibrary(LibraryCategory.Artists) },
                    TileSpec("PLAYLISTS", plural(counts.playlists, "playlist")) { navigator.openTab(SonaraDestination.Playlists) },
                    TileSpec("FOLDERS", plural(counts.folders, "folder")) { navigator.openTab(SonaraDestination.Folders) },
                )
                tiles.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s8)) {
                        pair.forEach { tile ->
                            SonaraTile(tile.label, tile.value, tile.onClick, Modifier.weight(1f))
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item { Section("CONTINUE LISTENING") }
        item {
            if (content.continueListening.isEmpty()) {
                Text(
                    "Tracks you play will appear here.",
                    style = typography.body,
                    color = colors.foregroundMuted,
                    modifier = Modifier.padding(horizontal = SonaraSpacing.s16),
                )
            } else {
                val ids = remember(content.continueListening) { content.continueListening.map { it.id } }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = SonaraSpacing.s16),
                    horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
                ) {
                    itemsIndexed(content.continueListening, key = { _, t -> t.id }, contentType = { _, _ -> "recent" }) { index, track ->
                        RecentCard(track, isCurrent = currentTrackId == track.id) { actions.play(ids, index) }
                    }
                }
            }
        }

        item { Section("RECENTLY ADDED") }
        itemsIndexed(content.recentlyAdded, key = { _, t -> "added-${t.id}" }, contentType = { _, _ -> "song" }) { index, track ->
            SonaraTrackRow(
                title = track.title,
                subtitle = "${track.artist} · ${track.album}",
                durationMs = track.durationMs,
                artworkTrackId = track.id,
                isCurrent = currentTrackId == track.id,
                onClick = { actions.play(addedIds, index) },
                menu = rememberTrackMenu(track, actions, navigator),
            )
        }
    }
}

private class TileSpec(val label: String, val value: String, val onClick: () -> Unit)

@Composable
private fun Section(title: String) {
    SonaraSectionHeader(title = title, modifier = Modifier.padding(horizontal = SonaraSpacing.s16))
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(value.toString(), style = SonaraTheme.typography.section, color = SonaraTheme.colors.accent, maxLines = 1)
        Text(label, style = SonaraTheme.typography.microLabel, color = SonaraTheme.colors.foregroundMuted, maxLines = 1)
    }
}

@Composable
private fun RecentCard(track: TrackEntity, isCurrent: Boolean, onClick: () -> Unit) {
    val colors = SonaraTheme.colors
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s4),
    ) {
        SonaraArtwork(trackId = track.id, size = 120.dp)
        Text(
            track.title, style = SonaraTheme.typography.trackTitle,
            color = if (isCurrent) colors.accent else colors.foreground,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(track.artist, style = SonaraTheme.typography.metadata, color = colors.foregroundMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
