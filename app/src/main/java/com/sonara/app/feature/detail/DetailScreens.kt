package com.sonara.app.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.app.core.common.formatTotalDuration
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.SonaraAlbumCard
import com.sonara.app.core.design.components.SonaraArtwork
import com.sonara.app.core.design.components.SonaraButton
import com.sonara.app.core.design.components.SonaraEmptyState
import com.sonara.app.core.design.components.SonaraErrorState
import com.sonara.app.core.design.components.SonaraLoadingState
import com.sonara.app.core.design.components.SonaraSectionHeader
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.design.components.SonaraTrackRow
import com.sonara.app.core.design.components.plural
import com.sonara.app.core.navigation.LocalSonaraNavigator
import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.model.FolderNames
import com.sonara.app.data.model.Genre
import com.sonara.app.feature.common.BrowseViewModel
import com.sonara.app.feature.common.UiState
import com.sonara.app.feature.common.appViewModel
import com.sonara.app.feature.common.rememberPlaybackActions
import com.sonara.app.feature.common.rememberCurrentTrackId
import com.sonara.app.feature.common.rememberTrackMenu

/** Which context a track list is shown in; decides numbering, subtitle and which menu entries make sense. */
private enum class TrackContext { Album, Artist, Mixed }

@Composable
fun AlbumDetailScreen(albumId: Long) {
    val viewModel = appViewModel { AlbumDetailViewModel(it.libraryRepository, albumId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailFrame("ALBUM", state, viewModel) { detail ->
        val album = detail.album
        if (album == null || detail.tracks.isEmpty()) {
            SonaraEmptyState("ALBUM NOT FOUND", "This album is no longer in your library.")
        } else {
            val meta = buildList {
                if (album.year > 0) add(album.year.toString())
                add(plural(album.trackCount, "track").uppercase())
                add(formatTotalDuration(album.durationMs))
            }.joinToString(" · ")
            CollectionList(
                title = album.title,
                subtitle = album.artist,
                meta = meta,
                artworkTrackId = album.artworkTrackId,
                tracks = detail.tracks,
                context = TrackContext.Album,
            )
        }
    }
}

@Composable
fun ArtistDetailScreen(name: String) {
    val viewModel = appViewModel { ArtistDetailViewModel(it.libraryRepository, name) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalSonaraNavigator.current
    DetailFrame("ARTIST", state, viewModel) { detail ->
        if (detail.tracks.isEmpty()) {
            SonaraEmptyState("ARTIST NOT FOUND", "No tracks by this artist are in your library any more.")
        } else {
            val meta = "${plural(detail.albums.size, "album")} · ${plural(detail.tracks.size, "track")}".uppercase()
            CollectionList(
                title = name,
                subtitle = null,
                meta = meta,
                artworkTrackId = detail.tracks.first().id,
                tracks = detail.tracks,
                context = TrackContext.Artist,
                beforeTracks = {
                    if (detail.albums.isNotEmpty()) {
                        item { SonaraSectionHeader("ALBUMS", Modifier.padding(horizontal = SonaraSpacing.s16)) }
                        item {
                            LazyRow(contentPadding = PaddingValues(horizontal = SonaraSpacing.s8)) {
                                items(detail.albums, key = { it.id }, contentType = { "album" }) { album ->
                                    SonaraAlbumCard(
                                        title = album.title,
                                        artist = if (album.year > 0) album.year.toString() else album.artist,
                                        trackCount = album.trackCount,
                                        artworkTrackId = album.artworkTrackId,
                                        onClick = { navigator.openAlbum(album.id) },
                                        modifier = Modifier.width(156.dp),
                                    )
                                }
                            }
                        }
                    }
                    item { SonaraSectionHeader("TRACKS", Modifier.padding(horizontal = SonaraSpacing.s16)) }
                },
            )
        }
    }
}

@Composable
fun GenreDetailScreen(genre: String?) {
    val viewModel = appViewModel { TrackCollectionViewModel(it.libraryRepository.genreTracks(genre)) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailFrame("GENRE", state, viewModel) { tracks ->
        if (tracks.isEmpty()) {
            SonaraEmptyState("GENRE EMPTY", "No tracks with this genre are in your library any more.")
        } else {
            CollectionList(
                title = genre ?: Genre.UNKNOWN_LABEL,
                subtitle = null,
                meta = "${plural(tracks.size, "track")} · ${formatTotalDuration(tracks.sumOf { it.durationMs })}".uppercase(),
                artworkTrackId = null,
                tracks = tracks,
                context = TrackContext.Mixed,
            )
        }
    }
}

/**
 * A folder's audio. [prefix] is the folder path; with [recursive] it also covers subfolders
 * (used for folders the person granted), otherwise only files directly inside it.
 */
@Composable
fun FolderDetailScreen(prefix: String, recursive: Boolean) {
    val viewModel = appViewModel {
        TrackCollectionViewModel(
            if (recursive) it.libraryRepository.treeTracks(prefix) else it.libraryRepository.folderTracks(prefix),
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailFrame("FOLDER", state, viewModel) { tracks ->
        if (tracks.isEmpty()) {
            SonaraEmptyState(
                "NO INDEXED AUDIO HERE",
                "SONARA has not indexed any audio files in this folder. If you just added music, rescan from the Library tab.",
            )
        } else {
            val name = FolderNames.displayName(prefix.ifEmpty { "/" }).let { if (prefix.isEmpty()) "Whole storage" else it }
            CollectionList(
                title = name,
                subtitle = prefix.ifEmpty { "Storage root" },
                meta = "${plural(tracks.size, "track")} · ${formatTotalDuration(tracks.sumOf { it.durationMs })}".uppercase(),
                artworkTrackId = null,
                tracks = tracks,
                context = TrackContext.Mixed,
            )
        }
    }
}

/** Top bar with back, plus loading and error handling shared by every detail screen. */
@Composable
private fun <T> DetailFrame(
    barTitle: String,
    state: UiState<T>,
    viewModel: BrowseViewModel,
    content: @Composable (T) -> Unit,
) {
    val navigator = LocalSonaraNavigator.current
    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(title = barTitle, onBack = navigator::back)
        when (state) {
            UiState.Loading -> SonaraLoadingState()
            is UiState.Error -> SonaraErrorState(state.message, onRetry = viewModel::retry)
            is UiState.Content -> content(state.data)
        }
    }
}

@Composable
private fun CollectionList(
    title: String,
    subtitle: String?,
    meta: String,
    artworkTrackId: Long?,
    tracks: List<TrackEntity>,
    context: TrackContext,
    beforeTracks: (LazyListScope.() -> Unit)? = null,
) {
    val navigator = LocalSonaraNavigator.current
    val actions = rememberPlaybackActions()
    val currentTrackId by rememberCurrentTrackId()
    val ids = remember(tracks) { tracks.map { it.id } }
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(SonaraSpacing.s16),
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s16),
            ) {
                if (artworkTrackId != null) SonaraArtwork(trackId = artworkTrackId, size = 112.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s4)) {
                    Text(title, style = typography.screenTitle, color = colors.foreground, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) {
                        Text(subtitle, style = typography.body, color = colors.foregroundMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text(meta, style = typography.microLabel, color = colors.accent)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SonaraSpacing.s16),
                horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s8),
            ) {
                SonaraButton("PLAY", onClick = { actions.play(ids, 0) }, emphasized = true, modifier = Modifier.weight(1f))
                SonaraButton("SHUFFLE", onClick = { actions.shuffle(ids) }, modifier = Modifier.weight(1f))
                SonaraButton("ADD TO QUEUE", onClick = { actions.addToQueue(ids, title) }, modifier = Modifier.weight(1.4f))
            }
        }
        if (beforeTracks != null) beforeTracks() else item { Spacer(Modifier.height(SonaraSpacing.s8)) }
        itemsIndexed(items = tracks, key = { _, t -> t.id }, contentType = { _, _ -> "song" }) { index, track ->
            SonaraTrackRow(
                title = track.title,
                subtitle = when (context) {
                    TrackContext.Album -> track.artist
                    TrackContext.Artist -> track.album
                    TrackContext.Mixed -> "${track.artist} · ${track.album}"
                },
                durationMs = track.durationMs,
                artworkTrackId = track.id,
                leadingNumber = if (context == TrackContext.Album) track.trackNumber else null,
                isCurrent = currentTrackId == track.id,
                onClick = { actions.play(ids, index) },
                menu = rememberTrackMenu(
                    track, actions, navigator,
                    showAlbum = context != TrackContext.Album,
                    showArtist = context != TrackContext.Artist,
                ),
            )
        }
    }
}
