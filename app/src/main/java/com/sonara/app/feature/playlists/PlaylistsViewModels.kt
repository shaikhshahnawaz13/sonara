package com.sonara.app.feature.playlists

import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sonara.app.core.common.formatTotalDuration
import com.sonara.app.core.design.components.plural
import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.model.PlaylistEntry
import com.sonara.app.data.model.PlaylistSummary
import com.sonara.app.data.repository.AddResult
import com.sonara.app.data.repository.LibraryRepository
import com.sonara.app.data.repository.NameResult
import com.sonara.app.data.repository.PlaylistRepository
import com.sonara.app.feature.common.BrowseViewModel
import com.sonara.app.feature.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/** "12 TRACKS · 45 MIN", plus how many entries have lost their file. Shared by the list and the detail header. */
internal fun playlistMeta(playlist: PlaylistSummary): String = buildList {
    add(plural(playlist.trackCount, "track"))
    if (playlist.trackCount > 0) add(formatTotalDuration(playlist.durationMs))
    if (playlist.unavailableCount > 0) add("${playlist.unavailableCount} unavailable")
}.joinToString(" · ").uppercase()

class PlaylistsViewModel(private val repository: PlaylistRepository) : BrowseViewModel() {
    val state = repository.playlists.asUiState()

    fun create(name: String, onResult: (NameResult) -> Unit) {
        viewModelScope.launch { onResult(repository.create(name)) }
    }

    fun rename(playlistId: Long, name: String, onResult: (NameResult) -> Unit) {
        viewModelScope.launch { onResult(repository.rename(playlistId, name)) }
    }

    fun delete(playlistId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            if (repository.delete(playlistId)) onDone()
        }
    }

    suspend fun playableIds(playlistId: Long): List<Long> = repository.playableTrackIds(playlistId)
}

data class PlaylistDetail(val summary: PlaylistSummary?, val entries: List<PlaylistEntry>)

/** The one definition of a playlist screen's data, shared by the screen and by [PlaylistPrefetch]. */
internal fun playlistDetailFlow(repository: PlaylistRepository, playlistId: Long) =
    combine(repository.playlist(playlistId), repository.entries(playlistId)) { summary, entries ->
        PlaylistDetail(summary, entries)
    }

class PlaylistDetailViewModel(
    private val repository: PlaylistRepository,
    private val playlistId: Long,
    /** Data fetched before the screen opened, so the first frame already has content; null opens as "loading". */
    prefetched: PlaylistDetail? = null,
) : BrowseViewModel() {
    val state = playlistDetailFlow(repository, playlistId)
        .asUiState(initial = prefetched?.let { UiState.Content(it) } ?: UiState.Loading)

    fun removeEntry(entryId: Long) {
        viewModelScope.launch { repository.removeEntry(playlistId, entryId) }
    }

    fun move(from: Int, to: Int) {
        viewModelScope.launch { repository.move(playlistId, from, to) }
    }

    fun rename(name: String, onResult: (NameResult) -> Unit) {
        viewModelScope.launch { onResult(repository.rename(playlistId, name)) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            if (repository.delete(playlistId)) onDone()
        }
    }

    /** Removes the playlist entries with these ids in one operation. Audio files are never touched. */
    fun removeEntries(entryIds: Collection<Long>, onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(repository.removeEntries(playlistId, entryIds)) }
    }

    fun removeUnavailable(onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(repository.removeUnavailable(playlistId)) }
    }
}

/** Everything the song picker shows: the library, which of its songs the playlist already holds, and the playlist itself. */
data class PickerData(
    val playlist: PlaylistSummary?,
    val tracks: List<TrackEntity>,
    val inPlaylist: Set<Long>,
) {
    /** Songs that can still be picked, in library order. */
    val addableIds: List<Long> = tracks.mapNotNull { if (it.id in inPlaylist) null else it.id }
}

class PlaylistSongPickerViewModel(
    library: LibraryRepository,
    private val playlists: PlaylistRepository,
    private val playlistId: Long,
) : BrowseViewModel() {
    val state = combine(library.tracks, playlists.playlist(playlistId), playlists.entries(playlistId)) { tracks, summary, entries ->
        PickerData(summary, tracks, entries.mapTo(HashSet()) { it.trackId })
    }.flowOn(Dispatchers.Default).asUiState()

    var selection by mutableStateOf(Selection.NONE)
        private set

    /** True while an add is running, so a double tap cannot add twice. */
    var adding by mutableStateOf(false)
        private set

    fun toggle(trackId: Long) {
        selection = selection.toggle(trackId)
    }

    fun selectAll(candidates: Collection<Long>) {
        selection = selection.selectAll(candidates)
    }

    fun deselectAll() {
        selection = selection.clear()
    }

    fun retainOnly(valid: Collection<Long>) {
        selection = selection.retainOnly(valid)
    }

    /** Appends the selected songs, in the order they were picked; songs the playlist already has are skipped. */
    fun addSelected(onDone: (AddResult) -> Unit) {
        val ids = selection.ids.toList()
        if (ids.isEmpty() || adding) return
        adding = true
        viewModelScope.launch {
            try {
                onDone(playlists.addTracks(playlistId, ids))
            } finally {
                adding = false
            }
        }
    }
}
