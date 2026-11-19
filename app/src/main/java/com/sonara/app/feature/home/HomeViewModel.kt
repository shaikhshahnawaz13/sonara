package com.sonara.app.feature.home

import androidx.lifecycle.viewModelScope
import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.repository.FolderRepository
import com.sonara.app.data.repository.LibraryRepository
import com.sonara.app.feature.common.BrowseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class LibraryCounts(
    val tracks: Int,
    val albums: Int,
    val artists: Int,
    val playlists: Int,
    val folders: Int,
)

data class HomeContent(
    val counts: LibraryCounts,
    val continueListening: List<TrackEntity>,
    val recentlyAdded: List<TrackEntity>,
)

class HomeViewModel(
    private val repository: LibraryRepository,
    folders: FolderRepository,
) : BrowseViewModel() {

    private val counts = combine(
        repository.trackCount,
        repository.albumCount,
        repository.artistCount,
        repository.playlistCount,
        folders.indexedFolders.map { it.size },
    ) { tracks, albums, artists, playlists, folderCount ->
        LibraryCounts(tracks, albums, artists, playlists, folderCount)
    }

    val content = combine(
        counts,
        repository.recentlyPlayed(CONTINUE_LIMIT),
        repository.recentlyAdded(RECENT_LIMIT),
    ) { counts, played, added ->
        HomeContent(counts, played, added)
    }.asUiState()

    val syncState = repository.syncState

    fun rescan() {
        viewModelScope.launch { repository.sync(force = true) }
    }

    private companion object {
        const val CONTINUE_LIMIT = 12
        const val RECENT_LIMIT = 10
    }
}
