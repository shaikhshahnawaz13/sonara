package com.sonara.app.feature.detail

import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.model.Album
import com.sonara.app.data.repository.LibraryRepository
import com.sonara.app.feature.common.BrowseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AlbumDetail(val album: Album?, val tracks: List<TrackEntity>)

class AlbumDetailViewModel(repository: LibraryRepository, albumId: Long) : BrowseViewModel() {
    val state = combine(repository.album(albumId), repository.albumTracks(albumId)) { album, tracks ->
        AlbumDetail(album, tracks)
    }.asUiState()
}

data class ArtistDetail(val albums: List<Album>, val tracks: List<TrackEntity>)

class ArtistDetailViewModel(repository: LibraryRepository, name: String) : BrowseViewModel() {
    val state = combine(repository.albumsOfArtist(name), repository.artistTracks(name)) { albums, tracks ->
        ArtistDetail(albums, tracks)
    }.asUiState()
}

/** A plain list of tracks: one genre, or one folder. */
class TrackCollectionViewModel(tracks: Flow<List<TrackEntity>>) : BrowseViewModel() {
    val state = tracks.asUiState()
}
