package com.sonara.app.feature.playlists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalDensity
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.design.components.LocalSnackbar
import com.sonara.app.core.design.components.artworkRequestPx
import com.sonara.app.core.design.components.SonaraConfirmDialog
import com.sonara.app.core.design.components.SonaraEmptyState
import com.sonara.app.core.design.components.SonaraErrorState
import com.sonara.app.core.design.components.SonaraIconButton
import com.sonara.app.core.design.components.SonaraIcons
import com.sonara.app.core.design.components.SonaraLoadingState
import com.sonara.app.core.design.components.SonaraMenuAction
import com.sonara.app.core.design.components.SonaraNameDialog
import com.sonara.app.core.design.components.SonaraPlaylistRow
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.design.components.plural
import com.sonara.app.core.navigation.LocalSonaraNavigator
import com.sonara.app.data.model.PlaylistSummary
import com.sonara.app.data.repository.NameResult
import com.sonara.app.feature.common.PlaybackActions
import com.sonara.app.feature.common.UiState
import com.sonara.app.feature.common.appViewModel
import com.sonara.app.feature.common.rememberPlaybackActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Sizes of the artwork on a playlist screen (header, and a song row); the prefetch warms exactly these. */
internal val PLAYLIST_HEADER_ARTWORK = 112.dp
internal val PLAYLIST_ROW_ARTWORK = 48.dp

private sealed interface PlaylistsDialog {
    data object Create : PlaylistsDialog
    data class Rename(val playlist: PlaylistSummary) : PlaylistsDialog
    data class Delete(val playlist: PlaylistSummary) : PlaylistsDialog
}

/** The Playlists tab: every playlist the person made, with real counts and artwork. */
@Composable
fun PlaylistsScreen() {
    val viewModel = appViewModel { PlaylistsViewModel(it.playlistRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalSonaraNavigator.current
    val actions = rememberPlaybackActions()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<PlaylistsDialog?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Opening a playlist first has its data and first-screen artwork put in memory (a few tens of milliseconds,
    // bounded), so the slide-in starts with the screen's content already there. See PlaylistPrefetch.
    val container = LocalAppContainer.current
    val density = LocalDensity.current
    val opening = remember { BooleanArray(1) }
    val openPlaylist: (Long) -> Unit = remember(container, navigator, density, scope) {
        { id ->
            if (!opening[0]) {
                opening[0] = true
                scope.launch {
                    try {
                        container.playlistPrefetch.warm(
                            playlistId = id,
                            headerPx = artworkRequestPx(PLAYLIST_HEADER_ARTWORK, density),
                            rowPx = artworkRequestPx(PLAYLIST_ROW_ARTWORK, density),
                        )
                    } finally {
                        opening[0] = false
                    }
                    navigator.openPlaylist(id)
                }
            }
        }
    }

    fun openDialog(next: PlaylistsDialog) {
        error = null
        dialog = next
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(
            title = "PLAYLISTS",
            actions = {
                SonaraIconButton(
                    icon = SonaraIcons.Add,
                    contentDescription = "Create playlist",
                    onClick = { openDialog(PlaylistsDialog.Create) },
                )
            },
        )
        when (val current = state) {
            UiState.Loading -> SonaraLoadingState()
            is UiState.Error -> SonaraErrorState(current.message, onRetry = viewModel::retry)
            is UiState.Content -> {
                val playlists = current.data
                if (playlists.isEmpty()) {
                    SonaraEmptyState(
                        title = "NO PLAYLISTS YET",
                        body = "Make a playlist, then add songs to it from any song, album, artist, folder or search result. Playlists stay on this device.",
                        actionLabel = "CREATE PLAYLIST",
                        onAction = { openDialog(PlaylistsDialog.Create) },
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(playlists, key = { it.id }, contentType = { "playlist" }) { playlist ->
                            val menu = remember(playlist, actions) {
                                playlistMenu(
                                    playlist = playlist,
                                    actions = actions,
                                    scope = scope,
                                    playableIds = viewModel::playableIds,
                                    onEmpty = { snackbar.show("NO PLAYABLE TRACKS IN ${playlist.name.uppercase()}") },
                                    onRename = { openDialog(PlaylistsDialog.Rename(playlist)) },
                                    onDelete = { openDialog(PlaylistsDialog.Delete(playlist)) },
                                )
                            }
                            SonaraPlaylistRow(
                                name = playlist.name,
                                subtitle = playlistMeta(playlist),
                                artworkTrackId = playlist.artworkTrackId,
                                onClick = { openPlaylist(playlist.id) },
                                menu = menu,
                            )
                        }
                    }
                }
            }
        }
    }

    when (val open = dialog) {
        null -> Unit
        PlaylistsDialog.Create -> SonaraNameDialog(
            title = "NEW PLAYLIST",
            initialName = "",
            confirmLabel = "CREATE",
            error = error,
            onNameEdited = { error = null },
            onConfirm = { name ->
                viewModel.create(name) { result ->
                    when (result) {
                        is NameResult.Saved -> {
                            dialog = null
                            navigator.openPlaylist(result.playlistId)
                        }
                        is NameResult.Rejected -> error = result.error.message
                    }
                }
            },
            onDismiss = { dialog = null },
        )
        is PlaylistsDialog.Rename -> SonaraNameDialog(
            title = "RENAME PLAYLIST",
            initialName = open.playlist.name,
            confirmLabel = "RENAME",
            error = error,
            onNameEdited = { error = null },
            onConfirm = { name ->
                viewModel.rename(open.playlist.id, name) { result ->
                    when (result) {
                        is NameResult.Saved -> {
                            dialog = null
                            snackbar.show("RENAMED TO ${result.name.uppercase()}")
                        }
                        is NameResult.Rejected -> error = result.error.message
                    }
                }
            },
            onDismiss = { dialog = null },
        )
        is PlaylistsDialog.Delete -> SonaraConfirmDialog(
            title = "DELETE PLAYLIST",
            body = "Delete \"${open.playlist.name}\"? Its ${plural(open.playlist.trackCount, "track")} stay on your device; only the playlist is removed.",
            confirmLabel = "DELETE",
            onConfirm = {
                viewModel.delete(open.playlist.id) {
                    dialog = null
                    snackbar.show("DELETED ${open.playlist.name.uppercase()}")
                }
            },
            onDismiss = { dialog = null },
        )
    }
}

private fun playlistMenu(
    playlist: PlaylistSummary,
    actions: PlaybackActions,
    scope: CoroutineScope,
    playableIds: suspend (Long) -> List<Long>,
    onEmpty: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
): List<SonaraMenuAction> {
    fun withIds(block: (List<Long>) -> Unit) {
        scope.launch {
            val ids = playableIds(playlist.id)
            if (ids.isEmpty()) onEmpty() else block(ids)
        }
    }
    return buildList {
        add(SonaraMenuAction("PLAY") { withIds { actions.play(it, 0) } })
        add(SonaraMenuAction("SHUFFLE") { withIds { actions.shuffle(it) } })
        add(SonaraMenuAction("ADD TO QUEUE") { withIds { actions.addToQueue(it, playlist.name) } })
        // The system Favourite playlist is managed by the app: no rename, no delete.
        if (!playlist.isSystem) {
            add(SonaraMenuAction("RENAME") { onRename() })
            add(SonaraMenuAction("DELETE") { onDelete() })
        }
    }
}
