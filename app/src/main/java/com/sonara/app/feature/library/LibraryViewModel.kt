package com.sonara.app.feature.library

import androidx.lifecycle.viewModelScope
import com.sonara.app.data.repository.LibraryRepository
import com.sonara.app.feature.common.BrowseViewModel
import com.sonara.app.data.local.preferences.SettingsRepository
import com.sonara.app.data.model.SongSorter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository,
    settings: SettingsRepository,
) : BrowseViewModel() {
    /** Songs in the person's chosen order. Sorting happens off the main thread and only when the list or the choice changes. */
    val songs = combine(repository.tracks, settings.libraryPrefs.map { it.defaultSort }.distinctUntilChanged()) { tracks, sort ->
        SongSorter.sort(tracks, sort)
    }.flowOn(Dispatchers.Default).asUiState()
    val albums = repository.albums.asUiState()
    val artists = repository.artists.asUiState()
    val genres = repository.genres.asUiState()
    val syncState = repository.syncState

    fun rescan() {
        viewModelScope.launch { repository.sync(force = true) }
    }
}
