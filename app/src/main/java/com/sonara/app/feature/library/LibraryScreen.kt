package com.sonara.app.feature.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.SonaraAlbumCard
import com.sonara.app.core.design.components.SonaraArtistRow
import com.sonara.app.core.design.components.SonaraErrorState
import com.sonara.app.core.design.components.SonaraIconButton
import com.sonara.app.core.design.components.SonaraListRow
import com.sonara.app.core.design.components.SonaraLoadingState
import com.sonara.app.core.design.components.SonaraPermissionPrompt
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.design.components.SonaraTrackRow
import com.sonara.app.core.design.components.plural
import com.sonara.app.core.navigation.LibraryCategory
import com.sonara.app.core.navigation.categorySlideDirection
import com.sonara.app.core.navigation.LocalSonaraNavigator
import com.sonara.app.core.permissions.AudioAccessState
import com.sonara.app.core.design.components.LocalSnackbar
import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.repository.SyncState
import com.sonara.app.feature.common.LibraryEmptyState
import com.sonara.app.feature.common.UiState
import com.sonara.app.feature.common.appViewModel
import com.sonara.app.feature.common.rememberPlaybackActions
import com.sonara.app.feature.common.rememberCurrentTrackId
import com.sonara.app.feature.common.rememberTrackMenu
import com.sonara.app.core.design.components.SonaraIcons

@Composable
fun LibraryScreen() {
    val container = LocalAppContainer.current
    val navigator = LocalSonaraNavigator.current
    val snackbar = LocalSnackbar.current
    val viewModel = appViewModel { LibraryViewModel(it.libraryRepository, it.settingsRepository) }
    val access by container.audioAccess.state.collectAsStateWithLifecycle()
    val sync by viewModel.syncState.collectAsStateWithLifecycle()
    val category by navigator.libraryCategory

    // Scroll positions live here, above the category switch, so they survive changing category and tab.
    val songsState = rememberLazyListState()
    val artistsState = rememberLazyListState()
    val genresState = rememberLazyListState()
    val albumsState = rememberLazyGridState()

    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(
            title = "LIBRARY",
            actions = {
                SonaraIconButton(SonaraIcons.Search, "Search", onClick = navigator::openSearch)
                SonaraIconButton(
                    icon = SonaraIcons.Refresh,
                    contentDescription = "Rescan library",
                    enabled = access == AudioAccessState.Granted && sync != SyncState.Scanning,
                    onClick = {
                        snackbar.show("RESCANNING LIBRARY")
                        viewModel.rescan()
                    },
                )
            },
        )
        if (access != AudioAccessState.Granted) {
            SonaraPermissionPrompt(access)
            return@Column
        }
        CategoryTabs(selected = category, onSelect = { navigator.libraryCategory.value = it })
        ScanStatus(sync)
        val motion = SonaraTheme.motion
        AnimatedContent(
            targetState = category,
            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            transitionSpec = {
                if (motion.isOff) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    // Moving to a later category: the old content leaves to the left, the new one enters from the right.
                    val direction = categorySlideDirection(initialState, targetState)
                    (slideInHorizontally(animationSpec = motion.categorySpec<IntOffset>()) { width -> width * direction } togetherWith
                        slideOutHorizontally(animationSpec = motion.categorySpec<IntOffset>()) { width -> -width * direction })
                        .using(null)
                }
            },
            label = "libraryCategory",
        ) { shown ->
            when (shown) {
                LibraryCategory.Songs -> SongsTab(viewModel, songsState)
                LibraryCategory.Albums -> AlbumsTab(viewModel, albumsState)
                LibraryCategory.Artists -> ArtistsTab(viewModel, artistsState)
                LibraryCategory.Genres -> GenresTab(viewModel, genresState)
            }
        }
    }
}

@Composable
private fun CategoryTabs(selected: LibraryCategory, onSelect: (LibraryCategory) -> Unit) {
    val colors = SonaraTheme.colors
    val count = LibraryCategory.entries.size
    // One lime bar travels between the labels. It is read only while placing, so the animation relayouts
    // a single small node and recomposes nothing.
    val indicator = animateFloatAsState(
        targetValue = selected.ordinal.toFloat(),
        animationSpec = SonaraTheme.motion.categorySpec<Float>(),
        label = "libraryIndicator",
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            LibraryCategory.entries.forEach { category ->
                CategoryTab(
                    category = category,
                    isSelected = category == selected,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(SonaraBorders.emphasis)
                .background(colors.borderMuted),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .layout { measurable, constraints ->
                    val cell = constraints.maxWidth.toFloat() / count
                    val placeable = measurable.measure(Constraints.fixed(cell.roundToInt(), SonaraBorders.emphasis.roundToPx()))
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.placeRelative((indicator.value * cell).roundToInt(), 0)
                    }
                }
                .background(colors.accent),
        )
    }
}

@Composable
private fun CategoryTab(
    category: LibraryCategory,
    isSelected: Boolean,
    onSelect: (LibraryCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) colors.accent else colors.foregroundMuted,
        animationSpec = SonaraTheme.motion.selectionColorSpec<Color>(),
        label = "libraryLabelColor",
    )
    Column(
        modifier = modifier
            .clickable(role = Role.Tab) { onSelect(category) }
            .semantics { this.selected = isSelected },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = SonaraSizes.minTouchTarget),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = category.label, style = SonaraTheme.typography.microLabel, color = labelColor)
        }
        // Reserves the space the shared indicator and rule are drawn in.
        Spacer(modifier = Modifier.height(SonaraBorders.emphasis))
    }
}

@Composable
private fun ScanStatus(sync: SyncState) {
    val colors = SonaraTheme.colors
    when (sync) {
        SyncState.Scanning -> Text(
            "SCANNING…", style = SonaraTheme.typography.microLabel, color = colors.accent,
            modifier = Modifier.padding(horizontal = SonaraSpacing.s16, vertical = SonaraSpacing.s8),
        )
        is SyncState.Failed -> Text(
            "SCAN FAILED · ${sync.message}", style = SonaraTheme.typography.microLabel, color = colors.danger,
            modifier = Modifier.padding(horizontal = SonaraSpacing.s16, vertical = SonaraSpacing.s8),
        )
        else -> Unit
    }
}

/** Shared loading / error / empty handling for the four tabs. */
@Composable
private fun <T> TabContent(
    state: UiState<List<T>>,
    viewModel: LibraryViewModel,
    content: @Composable (List<T>) -> Unit,
) {
    val sync by viewModel.syncState.collectAsStateWithLifecycle()
    when (state) {
        UiState.Loading -> SonaraLoadingState()
        is UiState.Error -> SonaraErrorState(state.message, onRetry = viewModel::retry)
        is UiState.Content -> if (state.data.isEmpty()) {
            LibraryEmptyState(sync = sync, onRescan = viewModel::rescan)
        } else {
            content(state.data)
        }
    }
}

@Composable
private fun SongsTab(viewModel: LibraryViewModel, listState: LazyListState) {
    val state by viewModel.songs.collectAsStateWithLifecycle()
    val navigator = LocalSonaraNavigator.current
    val actions = rememberPlaybackActions()
    val currentTrackId by rememberCurrentTrackId()
    TabContent(state, viewModel) { songs: List<TrackEntity> ->
        val ids = remember(songs) { songs.map { it.id } }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items = songs, key = { _, track -> track.id }, contentType = { _, _ -> "song" }) { index, track ->
                SonaraTrackRow(
                    title = track.title,
                    subtitle = "${track.artist} · ${track.album}",
                    durationMs = track.durationMs,
                    artworkTrackId = track.id,
                    isCurrent = currentTrackId == track.id,
                    onClick = { actions.play(ids, index) },
                    menu = rememberTrackMenu(track, actions, navigator),
                )
            }
        }
    }
}

@Composable
private fun AlbumsTab(viewModel: LibraryViewModel, gridState: LazyGridState) {
    val state by viewModel.albums.collectAsStateWithLifecycle()
    val navigator = LocalSonaraNavigator.current
    TabContent(state, viewModel) { albums ->
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = SonaraSpacing.s8),
        ) {
            items(items = albums, key = { it.id }, contentType = { "album" }) { album ->
                SonaraAlbumCard(
                    title = album.title,
                    artist = album.artist,
                    trackCount = album.trackCount,
                    artworkTrackId = album.artworkTrackId,
                    onClick = { navigator.openAlbum(album.id) },
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(viewModel: LibraryViewModel, listState: LazyListState) {
    val state by viewModel.artists.collectAsStateWithLifecycle()
    val navigator = LocalSonaraNavigator.current
    TabContent(state, viewModel) { artists ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(items = artists, key = { it.name }, contentType = { "artist" }) { artist ->
                SonaraArtistRow(
                    name = artist.name,
                    albumCount = artist.albumCount,
                    trackCount = artist.trackCount,
                    artworkTrackId = artist.artworkTrackId,
                    onClick = { navigator.openArtist(artist.name) },
                )
            }
        }
    }
}

@Composable
private fun GenresTab(viewModel: LibraryViewModel, listState: LazyListState) {
    val state by viewModel.genres.collectAsStateWithLifecycle()
    val navigator = LocalSonaraNavigator.current
    TabContent(state, viewModel) { genres ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(items = genres, key = { it.name ?: "~unknown~" }, contentType = { "genre" }) { genre ->
                SonaraListRow(
                    title = genre.displayName,
                    subtitle = plural(genre.trackCount, "track"),
                    onClick = { navigator.openGenre(genre.name) },
                )
            }
        }
    }
}
