package com.sonara.app.feature.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.LocalSnackbar
import com.sonara.app.core.design.components.SonaraButton
import com.sonara.app.core.design.components.SonaraDialog
import com.sonara.app.core.design.components.SonaraNameDialog
import com.sonara.app.core.design.components.SonaraPlaylistRow
import com.sonara.app.core.design.components.plural
import com.sonara.app.data.model.PlaylistSummary
import com.sonara.app.data.repository.AddResult
import com.sonara.app.data.repository.NameResult
import kotlinx.coroutines.launch

/** Tracks waiting to be put in a playlist. [label] names them in the dialog, e.g. a song or album title. */
data class AddToPlaylistRequest(val trackIds: List<Long>, val label: String)

/**
 * The single way every screen adds tracks to a playlist: call [add] and the app-level host shows the picker.
 * Song rows, album, artist, genre and folder screens and search results all go through it.
 */
@Stable
class PlaylistPicker {
    var request: AddToPlaylistRequest? by mutableStateOf(null)
        private set

    fun add(trackIds: List<Long>, label: String) {
        val ids = trackIds.distinct()
        if (ids.isNotEmpty()) request = AddToPlaylistRequest(ids, label)
    }

    internal fun close() {
        request = null
    }
}

val LocalPlaylistPicker = staticCompositionLocalOf { PlaylistPicker() }

/** What to tell the person after adding tracks. Pure, so it can be tested. */
internal fun addResultMessage(result: AddResult, playlistName: String): String {
    val name = playlistName.uppercase()
    return when {
        result.added == 0 -> "ALREADY IN $name"
        result.alreadyThere == 0 -> "ADDED ${plural(result.added, "track").uppercase()} TO $name"
        else -> "ADDED ${result.added} TO $name · ${result.alreadyThere} ALREADY THERE"
    }
}

/** Shows the picker and the new-playlist dialog for the current request. Placed once, at the app root. */
@Composable
fun PlaylistPickerHost(picker: PlaylistPicker) {
    val request = picker.request ?: return
    val repository = LocalAppContainer.current.playlistRepository
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val playlists by produceState<List<PlaylistSummary>?>(initialValue = null, repository) {
        repository.playlists.collect { value = it }
    }
    var creating by remember(request) { mutableStateOf(false) }
    var error by remember(request) { mutableStateOf<String?>(null) }

    if (creating) {
        SonaraNameDialog(
            title = "NEW PLAYLIST",
            initialName = "",
            confirmLabel = "CREATE",
            error = error,
            onNameEdited = { error = null },
            onConfirm = { name ->
                scope.launch {
                    when (val result = repository.createWith(name, request.trackIds)) {
                        is NameResult.Saved -> {
                            picker.close()
                            snackbar.show("ADDED ${plural(request.trackIds.size, "track").uppercase()} TO ${result.name.uppercase()}")
                        }
                        is NameResult.Rejected -> error = result.error.message
                    }
                }
            },
            onDismiss = { creating = false },
        )
        return
    }

    SonaraDialog(title = "ADD TO PLAYLIST", onDismiss = picker::close) {
        Text(
            text = request.label,
            style = SonaraTheme.typography.metadata,
            color = SonaraTheme.colors.foregroundMuted,
            maxLines = 1,
        )
        SonaraButton(label = "NEW PLAYLIST", onClick = { creating = true }, emphasized = true)
        val list = playlists
        when {
            list == null -> Unit
            list.isEmpty() -> Text(
                text = "You have no playlists yet. Create one to add these tracks.",
                style = SonaraTheme.typography.body,
                color = SonaraTheme.colors.foregroundMuted,
            )
            else -> LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s4),
            ) {
                items(list, key = { it.id }) { playlist ->
                    SonaraPlaylistRow(
                        name = playlist.name,
                        subtitle = plural(playlist.trackCount, "track"),
                        artworkTrackId = playlist.artworkTrackId,
                        onClick = {
                            scope.launch {
                                val result = repository.addTracks(playlist.id, request.trackIds)
                                picker.close()
                                snackbar.show(addResultMessage(result, playlist.name))
                            }
                        },
                    )
                }
            }
        }
        SonaraButton(label = "CANCEL", onClick = picker::close)
    }
}
