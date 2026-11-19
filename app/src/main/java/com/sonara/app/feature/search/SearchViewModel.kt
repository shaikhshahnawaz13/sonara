package com.sonara.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.data.local.database.PlaylistEntity
import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.local.preferences.SettingsRepository
import com.sonara.app.data.model.Album
import com.sonara.app.data.model.Artist
import com.sonara.app.data.model.Folder
import com.sonara.app.data.model.SearchEngine
import com.sonara.app.data.model.SearchResults
import com.sonara.app.data.repository.FolderRepository
import com.sonara.app.data.repository.LibraryRepository
import com.sonara.app.feature.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class SearchSources(
    val songs: List<TrackEntity>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val playlists: List<PlaylistEntity>,
    val folders: List<Folder>,
)

private class SearchRequest(val query: String, val sources: SearchSources)

/**
 * Search state. The query is held here so it survives rotation; matching runs off the main thread, is
 * debounced while typing and is cancelled when the query changes again.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    repository: LibraryRepository,
    folders: FolderRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val sources = combine(
        repository.tracks, repository.albums, repository.artists, repository.playlists, folders.indexedFolders,
    ) { songs, albums, artists, playlists, folderList ->
        SearchSources(songs, albums, artists, playlists, folderList)
    }

    /** Null while the query is empty. */
    val results: StateFlow<UiState<SearchResults?>> = combine(_query.debounce(DEBOUNCE_MS).distinctUntilChanged(), sources) { q, src -> SearchRequest(q, src) }
        .mapLatest { request ->
            val q = request.query
            val src = request.sources
            val state: UiState<SearchResults?> = if (SearchEngine.normalizeQuery(q).isEmpty()) {
                UiState.Content(null)
            } else {
                UiState.Content(
                    withContext(Dispatchers.Default) {
                        SearchEngine.search(q, src.songs, src.albums, src.artists, src.playlists, src.folders)
                    },
                )
            }
            state
        }
        .catch { emit(UiState.Error(it.message ?: "Search failed")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val recentSearches: StateFlow<List<String>> = settings.settings
        .map { it.recentSearches }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value.take(SearchEngine.MAX_QUERY_LENGTH)
    }

    /** Remember the current query as a recent search (when submitted or when a result is chosen). */
    fun commitQuery() {
        val q = _query.value
        viewModelScope.launch { settings.addRecentSearch(q) }
    }

    fun clearRecent() {
        viewModelScope.launch { settings.clearRecentSearches() }
    }

    private companion object {
        const val DEBOUNCE_MS = 150L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
