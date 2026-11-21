package com.sonara.app.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.LocalSnackbar
import com.sonara.app.core.design.components.SonaraButton
import com.sonara.app.core.design.components.SonaraEmptyState
import com.sonara.app.core.design.components.SonaraErrorState
import com.sonara.app.core.design.components.SonaraLoadingState
import com.sonara.app.core.design.components.SonaraPermissionPrompt
import com.sonara.app.core.design.components.SonaraTextAction
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.design.components.SonaraTrackRow
import com.sonara.app.core.navigation.LocalSonaraNavigator
import com.sonara.app.core.permissions.AudioAccessState
import com.sonara.app.feature.common.UiState
import com.sonara.app.feature.common.appViewModel

/**
 * Multi-select list of every song in the library, opened by BROWSE SONGS and ADD SONGS. Songs the playlist
 * already holds are shown but cannot be picked, so nothing is added twice. Nothing changes until ADD TO PLAYLIST.
 */
@Composable
fun PlaylistSongPickerScreen(playlistId: Long) {
    val container = LocalAppContainer.current
    val navigator = LocalSonaraNavigator.current
    val viewModel = appViewModel { PlaylistSongPickerViewModel(it.libraryRepository, it.playlistRepository, playlistId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val access by container.audioAccess.state.collectAsStateWithLifecycle()
    val playlistName = ((state as? UiState.Content)?.data?.playlist)?.name

    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(
            title = if (playlistName != null) "ADD TO ${playlistName.uppercase()}" else "ADD SONGS",
            onBack = navigator::back,
        )
        if (access != AudioAccessState.Granted) {
            SonaraPermissionPrompt(access)
            return@Column
        }
        when (val current = state) {
            UiState.Loading -> SonaraLoadingState(detail = "Reading your library")
            is UiState.Error -> SonaraErrorState(current.message, onRetry = viewModel::retry)
            is UiState.Content -> {
                val data = current.data
                when {
                    data.playlist == null -> SonaraEmptyState("PLAYLIST NOT FOUND", "This playlist no longer exists.")
                    data.tracks.isEmpty() -> SonaraEmptyState(
                        title = "NO SONGS FOUND",
                        body = "Songs from your indexed folders appear here once the library has been scanned.",
                    )
                    else -> PickerContent(data, viewModel)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.PickerContent(data: PickerData, viewModel: PlaylistSongPickerViewModel) {
    val navigator = LocalSonaraNavigator.current
    val snackbar = LocalSnackbar.current
    val colors = SonaraTheme.colors
    val selection = viewModel.selection
    val addable = data.addableIds

    // A song that stops being addable (it was added elsewhere, or left the library) leaves the selection.
    LaunchedEffect(addable) { viewModel.retainOnly(addable) }

    SelectionCountRow(count = selection.count) {
        SonaraTextAction(
            label = "SELECT ALL",
            enabled = addable.isNotEmpty() && !selection.coversAll(addable),
            onClick = { viewModel.selectAll(addable) },
        )
        SonaraTextAction(
            label = "DESELECT ALL",
            enabled = !selection.isEmpty,
            onClick = viewModel::deselectAll,
        )
    }
    BarDivider()
    if (addable.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().background(colors.surface).padding(SonaraSpacing.s16)) {
            Text(
                text = "EVERY SONG IS ALREADY IN THIS PLAYLIST",
                style = SonaraTheme.typography.microLabel,
                color = colors.foregroundMuted,
            )
        }
    }
    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
        items(items = data.tracks, key = { it.id }, contentType = { "song" }) { track ->
            val locked = track.id in data.inPlaylist
            SonaraTrackRow(
                title = track.title,
                subtitle = "${track.artist} · ${track.album}",
                durationMs = track.durationMs,
                artworkTrackId = track.id,
                selected = track.id in selection,
                locked = locked,
                lockedLabel = "IN PLAYLIST",
                onClick = { if (!locked) viewModel.toggle(track.id) },
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(SonaraBorders.standard).background(colors.borderMuted))
    Box(modifier = Modifier.fillMaxWidth().padding(SonaraSpacing.s16)) {
        SonaraButton(
            label = "ADD TO PLAYLIST",
            enabled = !selection.isEmpty && !viewModel.adding,
            emphasized = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val name = data.playlist?.name.orEmpty()
                viewModel.addSelected { result ->
                    snackbar.show(addResultMessage(result, name))
                    navigator.back()
                }
            },
        )
    }
}
