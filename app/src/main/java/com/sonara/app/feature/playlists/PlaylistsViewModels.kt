package com.sonara.app.feature.playlists

import androidx.lifecycle.viewModelScope
import com.sonara.app.core.common.formatTotalDuration
import com.sonara.app.core.design.components.plural
import com.sonara.app.data.model.PlaylistEntry
import com.sonara.app.data.model.PlaylistSummary
import com.sonara.app.data.repository.NameResult
import com.sonara.app.data.repository.PlaylistRepository
import com.sonara.app.feature.common.BrowseViewModel
import kotlinx.coroutines.flow.combine
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
            repository.delete(playlistId)
            onDone()
        }
    }

    suspend fun playableIds(playlistId: Long): List<Long> = repository.playableTrackIds(playlistId)
}

data class PlaylistDetail(val summary: PlaylistSummary?, val entries: List<PlaylistEntry>)

class PlaylistDetailViewModel(
    private val repository: PlaylistRepository,
    private val playlistId: Long,
) : BrowseViewModel() {
    val state = combine(repository.playlist(playlistId), repository.entries(playlistId)) { summary, entries ->
        PlaylistDetail(summary, entries)
    }.asUiState()

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
            repository.delete(playlistId)
            onDone()
        }
    }

    fun removeUnavailable(onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(repository.removeUnavailable(playlistId)) }
    }
}
