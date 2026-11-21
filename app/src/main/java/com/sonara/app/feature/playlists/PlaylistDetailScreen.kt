package com.sonara.app.feature.playlists

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntSize
import com.sonara.app.core.animation.SonaraMotion
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
import com.sonara.app.core.design.components.SonaraTextAction
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.design.components.SonaraTrackRow
import com.sonara.app.core.design.components.plural
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
    var editing by remember { mutableStateOf(false) }
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

    // Back leaves edit mode first. Nothing is changed by leaving: removals only happen on REMOVE SELECTED.
    BackHandler(enabled = editing) { editing = false }
    LaunchedEffect(summary == null) { if (summary == null) editing = false }

    Column(modifier = Modifier.fillMaxSize()) {
        // One top bar for both modes, so the bar itself never swaps: its title and EDIT/DONE slide into
        // each other, and the playlist menu makes room for DONE by folding away sideways.
        val motion = SonaraTheme.motion
        SonaraTopBar(
            title = {
                AnimatedContent(
                    targetState = editing,
                    transitionSpec = { slideSwap(targetState, motion) },
                    label = "playlistTitle",
                ) { isEditing ->
                    Text(
                        text = if (isEditing) "EDIT PLAYLIST" else "PLAYLIST",
                        style = SonaraTheme.typography.screenTitle,
                        color = SonaraTheme.colors.foreground,
                        maxLines = 1,
                    )
                }
            },
            onBack = if (editing) ({ editing = false }) else navigator::back,
            actions = {
                if (summary != null) {
                    AnimatedContent(
                        targetState = editing,
                        transitionSpec = { slideSwap(targetState, motion) },
                        label = "editDone",
                    ) { isEditing ->
                        SonaraTextAction(
                            label = if (isEditing) "DONE" else "EDIT",
                            onClick = { editing = !isEditing },
                        )
                    }
                }
                AnimatedVisibility(
                    visible = !editing && summary != null,
                    enter = expandHorizontally(animationSpec = motion.editSpec<IntSize>()),
                    exit = shrinkHorizontally(animationSpec = motion.editSpec<IntSize>()),
                ) {
                    SonaraOverflowMenu(actions = topMenu, contentDescription = "Playlist actions")
                }
            },
        )
        when (val current = state) {
            UiState.Loading -> SonaraLoadingState()
            is UiState.Error -> SonaraErrorState(current.message, onRetry = viewModel::retry)
            is UiState.Content -> {
                val detail = current.data
                if (detail.summary == null) {
                    SonaraEmptyState("PLAYLIST NOT FOUND", "This playlist no longer exists.")
                } else {
                    PlaylistBody(detail.summary, detail.entries, viewModel, editing)
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
    editing: Boolean,
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

    val motion2 = SonaraTheme.motion
    // Checkboxes: the column opens and closes with this value (read while measuring, not composing).
    val reveal = remember { Animatable(if (editing) 1f else 0f) }
    val revealValue = remember { { reveal.value } }
    var checksVisible by remember { mutableStateOf(editing) }
    LaunchedEffect(editing) {
        if (editing) {
            checksVisible = true
            reveal.animateTo(1f, motion2.editSpec())
        } else {
            reveal.animateTo(0f, motion2.editSpec())
            checksVisible = false
        }
    }
    var selection by remember { mutableStateOf(Selection.NONE) }
    var confirmRemove by remember { mutableStateOf(false) }
    // Leaving edit mode drops the selection; an entry that disappears drops out of it too.
    LaunchedEffect(editing) { if (!editing) selection = Selection.NONE }
    LaunchedEffect(entries) { selection = selection.retainOnly(entries.map { it.entryId }) }

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

    Column(modifier = Modifier.fillMaxSize()) {
    LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                AnimatedVisibility(
                    visible = !editing,
                    enter = expandVertically(animationSpec = motion2.editSpec<IntSize>()),
                    exit = shrinkVertically(animationSpec = motion2.editSpec<IntSize>()),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s8)) {
                        val hasTracks = playableIds.isNotEmpty()
                        SonaraButton("PLAY", onClick = { actions.play(playableIds, 0) }, enabled = hasTracks, emphasized = true, modifier = Modifier.weight(1f))
                        SonaraButton("SHUFFLE", onClick = { actions.shuffle(playableIds) }, enabled = hasTracks, modifier = Modifier.weight(1f))
                        SonaraButton("ADD TO QUEUE", onClick = { actions.addToQueue(playableIds, summary.name) }, enabled = hasTracks, modifier = Modifier.weight(1.4f))
                    }
                }
            }
        }
        if (shown.isEmpty()) {
            item(key = "empty") {
                SonaraEmptyState(
                    title = "THIS PLAYLIST IS EMPTY",
                    body = "Add songs from any song menu with ADD TO PLAYLIST, or from an album, artist, folder or search result.",
                    actionLabel = "BROWSE SONGS",
                    onAction = { navigator.openPlaylistSongPicker(summary.id) },
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
                    onClick = if (editing) {
                        { selection = selection.toggle(entry.entryId) }
                    } else {
                        { actions.play(playableIds, playableIndexOf[entry.entryId] ?: 0) }
                    },
                    selected = if (checksVisible) entry.entryId in selection else null,
                    selectionReveal = revealValue,
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
                    onClick = if (editing) {
                        { selection = selection.toggle(entry.entryId) }
                    } else {
                        { snackbar.show("THIS FILE IS NOT IN YOUR LIBRARY") }
                    },
                    selected = if (checksVisible) entry.entryId in selection else null,
                    selectionReveal = revealValue,
                    menu = extra,
                    trailing = handle,
                    modifier = rowModifier,
                )
            }
        }
    }
    val allIds = remember(shown) { shown.map { it.entryId } }
    // The list gives up the bar's height gradually, from the bottom, so it never jumps and its scroll position stays.
    AnimatedVisibility(
        visible = editing,
        enter = expandVertically(animationSpec = motion2.editSpec<IntSize>()),
        exit = shrinkVertically(animationSpec = motion2.editSpec<IntSize>()),
    ) {
        EditActionBar(
            selection = selection,
            allIds = allIds,
            onSelectAll = { selection = selection.selectAll(allIds) },
            onClear = { selection = selection.clear() },
            onRemove = { confirmRemove = true },
            onAddSongs = { navigator.openPlaylistSongPicker(summary.id) },
        )
    }
    }
    if (confirmRemove) {
        val count = selection.count
        SonaraConfirmDialog(
            title = "REMOVE FROM PLAYLIST",
            body = "Remove ${plural(count, "song")} from \"${summary.name}\"? Your audio files are not deleted; " +
                "only the playlist entries are removed.",
            confirmLabel = "REMOVE",
            onConfirm = {
                confirmRemove = false
                viewModel.removeEntries(selection.ids) { removed ->
                    selection = Selection.NONE
                    snackbar.show("REMOVED $removed FROM PLAYLIST")
                }
            },
            onDismiss = { confirmRemove = false },
        )
    }
}

/** The contextual area of edit mode: how many are selected, select/clear, and the two actions that use the selection. */
@Composable
private fun EditActionBar(
    selection: Selection,
    allIds: List<Long>,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onRemove: () -> Unit,
    onAddSongs: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(SonaraTheme.colors.background)) {
        BarDivider()
        SelectionCountRow(count = selection.count) {
            SonaraTextAction("SELECT ALL", onClick = onSelectAll, enabled = !selection.coversAll(allIds) && allIds.isNotEmpty())
            SonaraTextAction("CLEAR SELECTION", onClick = onClear, enabled = !selection.isEmpty)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = SonaraSpacing.s16, end = SonaraSpacing.s16, bottom = SonaraSpacing.s12),
            horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s8),
        ) {
            SonaraButton("REMOVE SELECTED", onClick = onRemove, enabled = !selection.isEmpty, modifier = Modifier.weight(1f))
            SonaraButton("ADD SONGS", onClick = onAddSongs, emphasized = true, modifier = Modifier.weight(1f))
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

/**
 * Slide the new content in from below and the old one out above when [entering] edit mode, and the other way
 * round when leaving. Clipped to the size of the slot; no fade.
 */
private fun AnimatedContentTransitionScope<Boolean>.slideSwap(entering: Boolean, motion: SonaraMotion): ContentTransform {
    val direction = if (entering) 1 else -1
    return (slideInVertically(animationSpec = motion.editSpec<IntOffset>()) { it * direction } togetherWith
        slideOutVertically(animationSpec = motion.editSpec<IntOffset>()) { -it * direction })
        .using(SizeTransform(clip = true) { _, _ -> motion.editSpec<IntSize>() })
}

private const val SETTLE_TIMEOUT_MS = 1_500L
private const val PLACEMENT_MS = 150
private val AUTO_SCROLL = 10.dp
private val EDGE = 72.dp
