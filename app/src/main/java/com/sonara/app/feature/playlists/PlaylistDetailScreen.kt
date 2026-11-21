package com.sonara.app.feature.playlists

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraSizes
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.LocalSnackbar
import com.sonara.app.core.design.components.SonaraArtwork
import com.sonara.app.core.design.components.SonaraButton
import com.sonara.app.core.design.components.SonaraConfirmDialog
import com.sonara.app.core.design.components.SonaraEmptyState
import com.sonara.app.core.design.components.SonaraErrorState
import com.sonara.app.core.design.components.SonaraIcons
import com.sonara.app.core.design.components.SonaraLoadingState
import com.sonara.app.core.design.components.SonaraMenuAction
import com.sonara.app.core.design.components.SonaraNameDialog
import com.sonara.app.core.design.components.SonaraOverflowMenu
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.design.components.SonaraTrackRow
import com.sonara.app.core.navigation.LibraryCategory
import com.sonara.app.core.navigation.LocalSonaraNavigator
import com.sonara.app.data.model.PlaylistEntry
import com.sonara.app.data.model.PlaylistSummary
import com.sonara.app.data.repository.NameResult
import com.sonara.app.feature.common.UiState
import com.sonara.app.feature.common.appViewModel
import com.sonara.app.feature.common.rememberCurrentTrackId
import com.sonara.app.feature.common.rememberPlaybackActions
import com.sonara.app.feature.common.rememberTrackMenu
import kotlinx.coroutines.delay

private enum class DetailDialog { Rename, Delete, RemoveUnavailable }

/** One playlist: play it, shuffle it, queue it, rename or delete it, and reorder or remove its songs. */
@Composable
fun PlaylistDetailScreen(playlistId: Long) {
    val viewModel = appViewModel { PlaylistDetailViewModel(it.playlistRepository, playlistId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalSonaraNavigator.current
    val snackbar = LocalSnackbar.current
    var dialog by remember { mutableStateOf<DetailDialog?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val summary = (state as? UiState.Content)?.data?.summary

    val topMenu = remember(summary?.name, summary?.unavailableCount) {
        buildList {
            if (summary != null) {
                add(SonaraMenuAction("RENAME") { error = null; dialog = DetailDialog.Rename })
                if (summary.unavailableCount > 0) {
                    add(SonaraMenuAction("REMOVE UNAVAILABLE") { dialog = DetailDialog.RemoveUnavailable })
                }
                add(SonaraMenuAction("DELETE") { dialog = DetailDialog.Delete })
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(
            title = "PLAYLIST",
            onBack = navigator::back,
            actions = { SonaraOverflowMenu(actions = topMenu, contentDescription = "Playlist actions") },
        )
        when (val current = state) {
            UiState.Loading -> SonaraLoadingState()
            is UiState.Error -> SonaraErrorState(current.message, onRetry = viewModel::retry)
            is UiState.Content -> {
                val detail = current.data
                if (detail.summary == null) {
                    SonaraEmptyState("PLAYLIST NOT FOUND", "This playlist no longer exists.")
                } else {
                    PlaylistBody(detail.summary, detail.entries, viewModel)
                }
            }
        }
    }

    when (dialog) {
        null -> Unit
        DetailDialog.Rename -> SonaraNameDialog(
            title = "RENAME PLAYLIST",
            initialName = summary?.name.orEmpty(),
            confirmLabel = "RENAME",
            error = error,
            onNameEdited = { error = null },
            onConfirm = { name ->
                viewModel.rename(name) { result ->
                    when (result) {
                        is NameResult.Saved -> {
                            dialog = null
                            snackbar.show("RENAMED TO ${result.name.uppercase()}")
                        }
                        is NameResult.Rejected -> error = result.error.message
                    }
                }
            },
            onDismiss = { dialog = null },
        )
        DetailDialog.Delete -> SonaraConfirmDialog(
            title = "DELETE PLAYLIST",
            body = "Delete \"${summary?.name.orEmpty()}\"? Its songs stay on your device; only the playlist is removed.",
            confirmLabel = "DELETE",
            onConfirm = {
                val name = summary?.name.orEmpty()
                viewModel.delete {
                    dialog = null
                    navigator.back()
                    snackbar.show("DELETED ${name.uppercase()}")
                }
            },
            onDismiss = { dialog = null },
        )
        DetailDialog.RemoveUnavailable -> SonaraConfirmDialog(
            title = "REMOVE UNAVAILABLE",
            body = "Remove the ${summary?.unavailableCount ?: 0} entries whose audio files are no longer in your library? " +
                "Nothing on your device is deleted.",
            confirmLabel = "REMOVE",
            onConfirm = {
                viewModel.removeUnavailable { removed ->
                    dialog = null
                    snackbar.show("REMOVED $removed UNAVAILABLE")
                }
            },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun PlaylistBody(
    summary: PlaylistSummary,
    entries: List<PlaylistEntry>,
    viewModel: PlaylistDetailViewModel,
) {
    val navigator = LocalSonaraNavigator.current
    val actions = rememberPlaybackActions()
    val snackbar = LocalSnackbar.current
    val currentTrackId by rememberCurrentTrackId()
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    val motion = SonaraTheme.motion
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    val listState = rememberLazyListState()
    val reorder = remember(listState) { EntryReorder(listState, headerCount = 1) }
    val shown = reorder.working ?: entries
    val playableIds = remember(shown) { shown.mapNotNull { it.track?.id } }
    val playableIndexOf = remember(shown) {
        buildMap<Long, Int> { shown.forEach { entry -> entry.track?.let { put(entry.entryId, size) } } }
    }

    // Keep the list's own order until the database shows the same one; never longer than a moment.
    LaunchedEffect(entries) { reorder.settle(entries) }
    LaunchedEffect(reorder.working, reorder.isDragging) {
        if (reorder.working != null && !reorder.isDragging) {
            delay(SETTLE_TIMEOUT_MS)
            reorder.settle(entries, force = true)
        }
    }
    LaunchedEffect(reorder.draggingKey) {
        if (reorder.draggingKey != null) {
            reorder.follow(
                autoScrollPx = with(density) { AUTO_SCROLL.toPx() },
                edgePx = with(density) { EDGE.toPx() },
            )
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(SonaraSpacing.s16),
                verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s16)) {
                    SonaraArtwork(trackId = summary.artworkTrackId, size = 112.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SonaraSpacing.s4)) {
                        Text(summary.name, style = typography.screenTitle, color = colors.foreground, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(playlistMeta(summary), style = typography.microLabel, color = colors.accent)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s8)) {
                    val hasTracks = playableIds.isNotEmpty()
                    SonaraButton("PLAY", onClick = { actions.play(playableIds, 0) }, enabled = hasTracks, emphasized = true, modifier = Modifier.weight(1f))
                    SonaraButton("SHUFFLE", onClick = { actions.shuffle(playableIds) }, enabled = hasTracks, modifier = Modifier.weight(1f))
                    SonaraButton("ADD TO QUEUE", onClick = { actions.addToQueue(playableIds, summary.name) }, enabled = hasTracks, modifier = Modifier.weight(1.4f))
                }
            }
        }
        if (shown.isEmpty()) {
            item(key = "empty") {
                SonaraEmptyState(
                    title = "THIS PLAYLIST IS EMPTY",
                    body = "Add songs from any song menu with ADD TO PLAYLIST, or from an album, artist, folder or search result.",
                    actionLabel = "BROWSE SONGS",
                    onAction = { navigator.openLibrary(LibraryCategory.Songs) },
                )
            }
        }
        itemsIndexed(
            items = shown,
            key = { _, entry -> entry.entryId },
            contentType = { _, entry -> if (entry.isAvailable) "song" else "missing" },
        ) { index, entry ->
            val isDragged = reorder.draggingKey == entry.entryId
            val rowModifier = if (isDragged) {
                Modifier
                    .zIndex(1f)
                    .graphicsLayer { translationY = reorder.translationOf(entry.entryId) }
                    .background(colors.surfaceRaised)
                    .border(SonaraBorders.standard, colors.accent)
            } else {
                Modifier.animateItem(
                    fadeInSpec = null,
                    placementSpec = if (motion.isOff) snap<IntOffset>() else tween<IntOffset>(PLACEMENT_MS),
                    fadeOutSpec = null,
                )
            }
            val last = shown.lastIndex
            val extra = remember(entry.entryId, index, last) {
                buildList {
                    add(SonaraMenuAction("REMOVE FROM PLAYLIST") { viewModel.removeEntry(entry.entryId) })
                    if (index > 0) add(SonaraMenuAction("MOVE UP") { viewModel.move(index, index - 1) })
                    if (index < last) add(SonaraMenuAction("MOVE DOWN") { viewModel.move(index, index + 1) })
                }
            }
            val track = entry.track
            val handle: @Composable () -> Unit = {
                DragHandle(
                    label = track?.title ?: "unavailable track",
                    onStart = {
                        if (reorder.start(entry.entryId, shown)) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = reorder::dragBy,
                    onEnd = {
                        reorder.finish()?.let { viewModel.move(it.from, it.to) }
                    },
                )
            }
            if (track != null) {
                SonaraTrackRow(
                    title = track.title,
                    subtitle = "${track.artist} · ${track.album}",
                    durationMs = track.durationMs,
                    artworkTrackId = track.id,
                    isCurrent = currentTrackId == track.id,
                    onClick = { actions.play(playableIds, playableIndexOf[entry.entryId] ?: 0) },
                    menu = rememberTrackMenu(track, actions, navigator, extra = extra),
                    trailing = handle,
                    modifier = rowModifier,
                )
            } else {
                SonaraTrackRow(
                    title = "Unavailable track",
                    subtitle = "Its file is no longer in your library",
                    durationMs = null,
                    artworkTrackId = null,
                    dimmed = true,
                    onClick = { snackbar.show("THIS FILE IS NOT IN YOUR LIBRARY") },
                    menu = extra,
                    trailing = handle,
                    modifier = rowModifier,
                )
            }
        }
    }
}

/** Grip at the end of a row. Dragging it moves the row; the same moves are in the row's menu for non-touch use. */
@Composable
private fun DragHandle(
    label: String,
    onStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
) {
    val start by rememberUpdatedState(onStart)
    val drag by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onEnd)
    Box(
        modifier = Modifier
            .size(SonaraSizes.minTouchTarget)
            .semantics { contentDescription = "Drag to reorder $label" }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start() },
                    onDrag = { change, amount ->
                        change.consume()
                        drag(amount.y)
                    },
                    onDragEnd = { end() },
                    onDragCancel = { end() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = SonaraIcons.Drag,
            contentDescription = null,
            tint = SonaraTheme.colors.foregroundMuted,
            modifier = Modifier.size(SonaraSizes.iconStandard),
        )
    }
}

private const val SETTLE_TIMEOUT_MS = 1_500L
private const val PLACEMENT_MS = 150
private val AUTO_SCROLL = 10.dp
private val EDGE = 72.dp
