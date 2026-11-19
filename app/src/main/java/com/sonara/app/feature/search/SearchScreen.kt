package com.sonara.app.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.SonaraArtistRow
import com.sonara.app.core.design.components.SonaraEmptyState
import com.sonara.app.core.design.components.SonaraErrorState
import com.sonara.app.core.design.components.SonaraFolderRow
import com.sonara.app.core.design.components.SonaraListRow
import com.sonara.app.core.design.components.SonaraLoadingState
import com.sonara.app.core.design.components.SonaraPermissionPrompt
import com.sonara.app.core.design.components.SonaraSearchField
import com.sonara.app.core.design.components.SonaraSectionHeader
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.design.components.SonaraTrackRow
import com.sonara.app.core.navigation.LocalSonaraNavigator
import com.sonara.app.core.navigation.SonaraDestination
import com.sonara.app.core.permissions.AudioAccessState
import com.sonara.app.data.model.SearchEngine
import com.sonara.app.data.model.SearchResults
import com.sonara.app.feature.common.UiState
import com.sonara.app.feature.common.appViewModel
import com.sonara.app.feature.common.rememberPlaybackActions
import com.sonara.app.feature.common.trackMenu

@Composable
fun SearchScreen() {
    val container = LocalAppContainer.current
    val navigator = LocalSonaraNavigator.current
    val viewModel = appViewModel { SearchViewModel(it.libraryRepository, it.folderRepository, it.settingsRepository) }
    val access by container.audioAccess.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val recent by viewModel.recentSearches.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(
            title = {
                SonaraSearchField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    onSearch = {
                        viewModel.commitQuery()
                        keyboard?.hide()
                    },
                    focusRequester = focusRequester,
                )
            },
            onBack = navigator::back,
        )
        if (access != AudioAccessState.Granted) {
            SonaraPermissionPrompt(access)
            return@Column
        }
        if (query.isBlank()) {
            RecentSearches(recent, onPick = viewModel::onQueryChange, onClear = viewModel::clearRecent)
            return@Column
        }
        when (val state = results) {
            UiState.Loading -> SonaraLoadingState(label = "SEARCHING")
            is UiState.Error -> SonaraErrorState(state.message, onRetry = null)
            is UiState.Content -> {
                val data = state.data
                when {
                    data == null -> SonaraLoadingState(label = "SEARCHING")
                    data.isEmpty -> SonaraEmptyState(
                        title = "NO RESULTS",
                        body = "Nothing on this device matches \"${data.query}\". Search looks at song, album, artist, playlist and folder names.",
                    )
                    else -> ResultsList(data, onChosen = viewModel::commitQuery)
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(recent: List<String>, onPick: (String) -> Unit, onClear: () -> Unit) {
    val colors = SonaraTheme.colors
    if (recent.isEmpty()) {
        SonaraEmptyState(
            title = "SEARCH YOUR MUSIC",
            body = "Search songs, albums, artists, playlists and folders on this device. Nothing is sent online.",
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SonaraSectionHeader(
                title = "RECENT SEARCHES",
                modifier = Modifier.padding(horizontal = SonaraSpacing.s16),
                actionLabel = "CLEAR",
                onAction = onClear,
            )
        }
        items(items = recent, key = { it }) { term ->
            Text(
                text = term,
                style = SonaraTheme.typography.trackTitle,
                color = colors.foreground,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onPick(term) }
                    .padding(horizontal = SonaraSpacing.s16, vertical = SonaraSpacing.s16),
            )
        }
    }
}

@Composable
private fun ResultsList(results: SearchResults, onChosen: () -> Unit) {
    val navigator = LocalSonaraNavigator.current
    val actions = rememberPlaybackActions()
    val playback by LocalAppContainer.current.playbackController.state.collectAsStateWithLifecycle()
    val songIds = remember(results.songs) { results.songs.map { it.id } }
    val pad = Modifier.padding(horizontal = SonaraSpacing.s16)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (results.songs.isNotEmpty()) {
            item(key = "h-songs") { SonaraSectionHeader("SONGS", pad) }
            itemsIndexed(results.songs, key = { _, t -> "s-${t.id}" }) { index, track ->
                SonaraTrackRow(
                    title = track.title,
                    subtitle = "${track.artist} · ${track.album}",
                    durationMs = track.durationMs,
                    artworkTrackId = track.id,
                    isCurrent = playback.currentTrackId == track.id,
                    onClick = {
                        onChosen()
                        actions.play(songIds, index)
                    },
                    menu = trackMenu(track, actions, navigator),
                )
            }
        }
        if (results.albums.isNotEmpty()) {
            item(key = "h-albums") { SonaraSectionHeader("ALBUMS", pad) }
            items(results.albums, key = { "a-${it.id}" }) { album ->
                SonaraListRow(
                    title = album.title,
                    subtitle = "${album.artist} · ${album.trackCount} tracks",
                    onClick = {
                        onChosen()
                        navigator.openAlbum(album.id)
                    },
                )
            }
        }
        if (results.artists.isNotEmpty()) {
            item(key = "h-artists") { SonaraSectionHeader("ARTISTS", pad) }
            items(results.artists, key = { "r-${it.name}" }) { artist ->
                SonaraArtistRow(
                    name = artist.name,
                    albumCount = artist.albumCount,
                    trackCount = artist.trackCount,
                    artworkTrackId = artist.artworkTrackId,
                    onClick = {
                        onChosen()
                        navigator.openArtist(artist.name)
                    },
                )
            }
        }
        if (results.playlists.isNotEmpty()) {
            item(key = "h-playlists") { SonaraSectionHeader("PLAYLISTS", pad) }
            items(results.playlists, key = { "p-${it.id}" }) { playlist ->
                SonaraListRow(
                    title = playlist.name,
                    subtitle = playlist.description ?: "Playlist",
                    onClick = {
                        onChosen()
                        navigator.openTab(SonaraDestination.Playlists)
                    },
                )
            }
        }
        if (results.folders.isNotEmpty()) {
            item(key = "h-folders") { SonaraSectionHeader("FOLDERS", pad) }
            items(results.folders, key = { "f-${it.path}" }) { folder ->
                SonaraFolderRow(
                    name = folder.name,
                    location = folder.path,
                    trackCount = folder.trackCount,
                    onClick = {
                        onChosen()
                        navigator.openFolder(folder.path, recursive = false)
                    },
                )
            }
        }
        if (results.truncated) {
            item(key = "truncated") {
                Text(
                    "SHOWING THE FIRST ${SearchEngine.MAX_PER_SECTION} OF EACH · REFINE YOUR SEARCH",
                    style = SonaraTheme.typography.microLabel,
                    color = SonaraTheme.colors.foregroundMuted,
                    modifier = Modifier.padding(SonaraSpacing.s16),
                )
            }
        }
    }
}
