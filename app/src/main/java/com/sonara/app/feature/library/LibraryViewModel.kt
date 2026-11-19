package com.sonara.app.feature.library

import androidx.lifecycle.viewModelScope
import com.sonara.app.data.repository.LibraryRepository
import com.sonara.app.feature.common.BrowseViewModel
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: LibraryRepository) : BrowseViewModel() {
    val songs = repository.tracks.asUiState()
    val albums = repository.albums.asUiState()
    val artists = repository.artists.asUiState()
    val genres = repository.genres.asUiState()
    val syncState = repository.syncState

    fun rescan() {
        viewModelScope.launch { repository.sync(force = true) }
    }
}
