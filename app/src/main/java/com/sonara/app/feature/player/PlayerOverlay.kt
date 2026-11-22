package com.sonara.app.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.sonara.app.core.animation.MotionTokens
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.common.formatDuration
import com.sonara.app.core.design.SonaraBorders
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.LocalSnackbar
import com.sonara.app.core.design.components.SonaraIconButton
import com.sonara.app.core.design.components.SonaraIcons
import com.sonara.app.core.design.components.SonaraPlayerArtwork
import com.sonara.app.playback.PlaybackController
import com.sonara.app.playback.PlaybackUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The Full Player, drawn as the mini-player grown to full screen. There is no separate screen: one surface
 * and its elements are placed between their mini-player rectangle and their Full Player rectangle by the
 * sheet's progress, in the layout and draw phases only, so nothing recomposes per frame. The elements that
 * exist in both (artwork, title, artist, play, next) travel; the rest are revealed in sequence.
 */
@Composable
fun PlayerOverlay(sheet: PlayerSheetState) {
    val container = LocalAppContainer.current
    val controller = container.playbackController
    val state by controller.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val motion by rememberUpdatedState(SonaraTheme.motion)

    // The last state that had a track. A reconnect (rotation, returning to the app) briefly has none; the
    // player stays open and keeps showing what it had instead of blinking.
    val lastWithMedia = remember { arrayOfNulls<PlaybackUiState>(1) }
    if (state.hasMedia) lastWithMedia[0] = state
    val shown = if (state.hasMedia) state else lastWithMedia[0]

    // A track that cannot be played is skipped by the service; say so instead of failing silently.
    val snackbar = LocalSnackbar.current
    LaunchedEffect(state.lastError) {
        if (state.lastError != null) {
            snackbar.show("COULD NOT PLAY A TRACK · SKIPPING")
            controller.clearError()
        }
    }

    // Only when connected and truly empty (the queue was cleared) is there nothing to show a player for.
    LaunchedEffect(state.connected, state.hasMedia) {
        if (state.connected && !state.hasMedia && sheet.overlayShown) sheet.closeNow()
    }

    BackHandler(enabled = sheet.expanded) {
        if (sheet.queueOpen) {
            sheet.queueOpen = false
        } else {
            scope.launch { sheet.collapse(motion.playerCollapseSpec()) }
        }
    }

    if (!sheet.overlayShown) return
    PlayerSurface(
        sheet = sheet,
        state = shown,
        controller = controller,
        onCollapse = { scope.launch { sheet.collapse(motion.playerCollapseSpec()) } },
    )
}


@Composable
private fun PlayerSurface(
    sheet: PlayerSheetState,
    state: PlaybackUiState?,
    controller: PlaybackController,
    onCollapse: () -> Unit,
) {
    val container = LocalAppContainer.current
    val colors = SonaraTheme.colors
    val typography = SonaraTheme.typography
    val density = LocalDensity.current
    val d = density.density
    val insetTop = WindowInsets.systemBars.getTop(density)
    val insetBottom = WindowInsets.systemBars.getBottom(density)
    val cache = remember { SlotsCache() }
    val dragFraction = remember { mutableStateOf<Float?>(null) }
    // Extras do nothing until the surface has landed, so a tap on a half-revealed control is not a command.
    val interactive by remember { derivedStateOf { sheet.progress.value >= INTERACTIVE_AT } }

    // The heart shows the Favourites playlist and nothing else. Until the database has answered it is neither
    // filled nor tappable, rather than guessing.
    val playlists = container.playlistRepository
    val favouriteIds: Set<Long>? by playlists.favouriteTrackIds.collectAsStateWithLifecycle(initialValue = null as Set<Long>?)
    val snackbar = LocalSnackbar.current
    val queue by controller.queue.collectAsStateWithLifecycle()
    val position = controller.positionMs.collectAsStateWithLifecycle()

    val titleStyle = typography.trackTitle.copy(fontSize = PlayerGeometry.FULL_TITLE_SP.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    val artistStyle = typography.metadata.copy(fontSize = PlayerGeometry.FULL_ARTIST_SP.sp, lineHeight = 22.sp)

    Box(modifier = Modifier.fillMaxSize()) {
        Layout(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val s = cache.slots ?: return@drawBehind
                    val e = sheet.progress.value.coerceIn(0f, 1f)
                    val l = PlayerGeometry.lerp(s.surfaceMini[0], s.surfaceFull[0], e)
                    val t = PlayerGeometry.lerp(s.surfaceMini[1], s.surfaceFull[1], e)
                    val r = PlayerGeometry.lerp(s.surfaceMini[2], s.surfaceFull[2], e)
                    val b = PlayerGeometry.lerp(s.surfaceMini[3], s.surfaceFull[3], e)
                    drawRect(lerp(colors.surfaceRaised, colors.background, e), Offset(l, t), Size(r - l, b - t))
                    // The mini-player's top rule thins out as the surface fills the screen.
                    drawRect(
                        colors.border.copy(alpha = 1f - e),
                        Offset(l, t),
                        Size(r - l, SonaraBorders.standard.toPx()),
                    )
                }
                // Everything under the surface stays inert while it is open.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent().changes.forEach { it.consume() }
                    }
                },
            content = {
                if (state != null) {
                    val trackId = state.currentTrackId
                    val showPause = state.showsPause
                    val buffering = state.isWaitingToPlay
                    val isFavourite = trackId != null && favouriteIds?.contains(trackId) == true

                    SonaraPlayerArtwork(trackId, Modifier.layoutId(PlayerSlot.Art), queueIndex = state.queueIndex)
                    Text(
                        text = state.title.orEmpty(),
                        style = titleStyle,
                        color = colors.foreground,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.layoutId(PlayerSlot.Title),
                    )
                    Text(
                        text = state.artist.orEmpty(),
                        style = artistStyle,
                        color = colors.foregroundMuted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.layoutId(PlayerSlot.Artist),
                    )
                    PlayerPlayButton(
                        showPause = showPause,
                        buffering = buffering,
                        onClick = controller::togglePlayPause,
                        progress = { sheet.progress.value },
                        modifier = Modifier.layoutId(PlayerSlot.Play),
                    )
                    SonaraIconButton(
                        icon = SonaraIcons.SkipNext,
                        contentDescription = "Next track",
                        onClick = controller::skipToNext,
                        enabled = state.hasNext,
                        modifier = Modifier.layoutId(PlayerSlot.Next),
                    )

                    // Revealed controls
                    Box(Modifier.layoutId(PlayerSlot.Album), contentAlignment = Alignment.CenterStart) {
                        Text(
                            text = state.album.orEmpty(),
                            style = typography.metadata,
                            color = colors.foregroundMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FavouriteHeart(
                        trackId = trackId,
                        isFavourite = isFavourite,
                        onClick = {
                            if (interactive && trackId != null && favouriteIds != null) {
                                container.appScope.launch {
                                    try {
                                        playlists.toggleFavourite(trackId)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        // Nothing was changed, and the icon follows the database, so it stays truthful.
                                        snackbar.show("COULD NOT UPDATE FAVOURITES")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.layoutId(PlayerSlot.Favourite),
                    )
                    SonaraIconButton(
                        icon = SonaraIcons.SkipPrevious,
                        contentDescription = "Previous track",
                        onClick = { if (interactive) controller.skipToPrevious() },
                        modifier = Modifier.layoutId(PlayerSlot.Previous),
                    )
                    SonaraIconButton(
                        icon = SonaraIcons.Shuffle,
                        contentDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
                        onClick = { if (interactive) controller.setShuffle(!state.shuffleEnabled) },
                        tint = if (state.shuffleEnabled) colors.accent else colors.foregroundMuted,
                        animateChange = true,
                        modifier = Modifier.layoutId(PlayerSlot.Shuffle),
                    )
                    SonaraIconButton(
                        icon = if (state.repeat.showsOneGlyph) SonaraIcons.RepeatOne else SonaraIcons.Repeat,
                        contentDescription = state.repeat.contentDescription,
                        onClick = { if (interactive) controller.cycleRepeat() },
                        tint = if (state.repeat.isActive) colors.accent else colors.foregroundMuted,
                        animateChange = true,
                        modifier = Modifier.layoutId(PlayerSlot.Repeat),
                    )
                    PlayerSeekBar(
                        position = position,
                        durationMs = { state.durationMs },
                        enabled = { interactive },
                        dragFraction = dragFraction,
                        onSeek = controller::seekTo,
                        modifier = Modifier.layoutId(PlayerSlot.Seek),
                    )
                    ElapsedTime(position, state.durationMs, dragFraction, Modifier.layoutId(PlayerSlot.TimeStart))
                    Box(Modifier.layoutId(PlayerSlot.TimeEnd), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = if (state.durationMs > 0L) formatDuration(state.durationMs) else "--:--",
                            style = typography.metadata,
                            color = colors.foregroundMuted,
                            maxLines = 1,
                        )
                    }
                    SonaraIconButton(
                        icon = SonaraIcons.ChevronDown,
                        contentDescription = "Collapse player",
                        onClick = { if (interactive) onCollapse() },
                        modifier = Modifier.layoutId(PlayerSlot.Collapse),
                    )
                    SonaraIconButton(
                        icon = SonaraIcons.Queue,
                        contentDescription = "Open queue",
                        onClick = { if (interactive) sheet.queueOpen = true },
                        modifier = Modifier.layoutId(PlayerSlot.Queue),
                    )
                    Box(Modifier.layoutId(PlayerSlot.Label), contentAlignment = Alignment.CenterStart) {
                        Text(
                            text = if (buffering) "BUFFERING" else "NOW PLAYING",
                            style = typography.microLabel,
                            color = colors.accent,
                            maxLines = 1,
                        )
                    }
                }
            },
        ) { measurables, constraints ->
            val w = constraints.maxWidth
            val h = constraints.maxHeight
            val slots = cache.get(w.toFloat(), h.toFloat(), insetTop.toFloat(), insetBottom.toFloat(), d, sheet.miniBounds)
            val e = sheet.progress.value.coerceIn(0f, 1f)
            val n = measurables.size
            val placeables = arrayOfNulls<Placeable>(n)
            val xs = IntArray(n)
            val ys = IntArray(n)
            val alphas = FloatArray(n)
            val shifts = FloatArray(n)
            val scales = FloatArray(n)
            for (k in 0 until n) {
                val slot = measurables[k].layoutId as? PlayerSlot ?: continue
                val i = slot.ordinal * 4
                val l = PlayerGeometry.lerp(slots.mini[i], slots.full[i], e)
                val t = PlayerGeometry.lerp(slots.mini[i + 1], slots.full[i + 1], e)
                val r = PlayerGeometry.lerp(slots.mini[i + 2], slots.full[i + 2], e)
                val b = PlayerGeometry.lerp(slots.mini[i + 3], slots.full[i + 3], e)
                val reveal = if (slot.ordinal < PlayerSlot.sharedCount) 1f else PlayerGeometry.phase(e, PlayerGeometry.revealStart(slot), PlayerGeometry.REVEAL_LENGTH)
                alphas[k] = reveal
                shifts[k] = (1f - reveal) * PlayerGeometry.REVEAL_SHIFT * d
                scales[k] = 1f
                if (slot == PlayerSlot.Title || slot == PlayerSlot.Artist) {
                    val from = if (slot == PlayerSlot.Title) PlayerGeometry.MINI_TITLE_SCALE else PlayerGeometry.MINI_ARTIST_SCALE
                    val s = PlayerGeometry.lerp(from, 1f, e)
                    val p = measurables[k].measure(Constraints(maxWidth = ((r - l) / s).roundToInt().coerceAtLeast(0)))
                    placeables[k] = p
                    scales[k] = s
                    xs[k] = l.roundToInt()
                    // Centre the scaled line in its slot so the glyphs sit where the mini-player's do.
                    ys[k] = (t + ((b - t) - p.height * s) / 2f).roundToInt()
                } else {
                    val pw = (r - l).roundToInt().coerceAtLeast(0)
                    val ph = (b - t).roundToInt().coerceAtLeast(0)
                    placeables[k] = measurables[k].measure(Constraints.fixed(pw, ph))
                    xs[k] = l.roundToInt()
                    ys[k] = t.roundToInt()
                }
            }
            layout(w, h) {
                for (k in 0 until n) {
                    val p = placeables[k] ?: continue
                    val alpha = alphas[k]
                    val shift = shifts[k]
                    val scale = scales[k]
                    p.placeWithLayer(xs[k], ys[k]) {
                        this.alpha = alpha
                        translationY = shift
                        if (scale != 1f) {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    }
                }
            }
        }

        PlayerQueuePanel(
            visible = sheet.queueOpen,
            queue = queue,
            currentIndex = state?.queueIndex ?: -1,
            onClose = { sheet.queueOpen = false },
            onJump = { index ->
                controller.skipToQueueItem(index)
                sheet.queueOpen = false
            },
            onRemove = controller::removeQueueItem,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Elapsed time. Recomposes when the displayed second changes, not on every position tick. */
@Composable
private fun ElapsedTime(
    position: State<Long>,
    durationMs: Long,
    dragFraction: State<Float?>,
    modifier: Modifier,
) {
    val seconds by remember(durationMs) {
        derivedStateOf {
            val ms = dragFraction.value?.let { PlayerGeometry.positionForFraction(it, durationMs) } ?: position.value
            ms / 1000L
        }
    }
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Text(
            text = formatDuration(seconds * 1000L),
            style = SonaraTheme.typography.metadata,
            color = SonaraTheme.colors.foregroundMuted,
            maxLines = 1,
        )
    }
}

/** Keeps the computed slots until an input changes, so a frame costs arithmetic only. */
private class SlotsCache {
    var slots: PlayerSlots? = null
        private set
    private var w = -1f
    private var h = -1f
    private var top = -1f
    private var bottom = -1f
    private var d = -1f
    private var bounds: FloatArray? = null

    fun get(width: Float, height: Float, insetTop: Float, insetBottom: Float, density: Float, miniBounds: FloatArray?): PlayerSlots {
        val cached = slots
        if (cached != null && w == width && h == height && top == insetTop && bottom == insetBottom && d == density && bounds === miniBounds) {
            return cached
        }
        w = width; h = height; top = insetTop; bottom = insetBottom; d = density; bounds = miniBounds
        return PlayerGeometry.compute(width, height, insetTop, insetBottom, density, miniBounds).also { slots = it }
    }
}

private const val INTERACTIVE_AT = 0.98f

/**
 * The heart. The icon and its size never change, so nothing around it moves. The colour eases between muted and
 * accent, and a small, quiet pulse (a few percent, no overshoot) confirms a change of this track's membership.
 * Both are driven by the database-backed [isFavourite], so a tap that failed shows nothing. Colour and pulse
 * are read only while drawing or in this small scope, so the player's own animation is not touched, and a
 * track change or a change made elsewhere updates the colour without the pulse.
 */
@Composable
private fun FavouriteHeart(
    trackId: Long?,
    isFavourite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonaraTheme.colors
    val motion = SonaraTheme.motion
    val tint by animateColorAsState(
        targetValue = if (isFavourite) colors.accent else colors.foreground,
        animationSpec = motion.selectionColorSpec(),
        label = "favouriteTint",
    )
    val pulse = remember { Animatable(1f) }
    val last = remember { HeartMemory() }
    LaunchedEffect(trackId, isFavourite) {
        val pulses = heartShouldPulse(last.seen, last.trackId, last.isFavourite, trackId, isFavourite)
        last.seen = true
        last.trackId = trackId
        last.isFavourite = isFavourite
        if (pulses && !motion.isOff) {
            pulse.animateTo(HEART_PULSE_SCALE, tween(motion.durationMs(HEART_PULSE_UP_MS), easing = MotionTokens.StandardEasing))
            pulse.animateTo(1f, tween(motion.durationMs(HEART_PULSE_DOWN_MS), easing = MotionTokens.StandardEasing))
        }
    }
    SonaraIconButton(
        icon = if (isFavourite) SonaraIcons.HeartFilled else SonaraIcons.Heart,
        contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
        onClick = onClick,
        tint = tint,
        modifier = modifier.graphicsLayer {
            scaleX = pulse.value
            scaleY = pulse.value
        },
    )
}

/**
 * Whether the heart pulses: only when the same track's membership changed since the last look. The first look,
 * a change of track and a change made elsewhere that arrives with a track change all update the colour quietly.
 */
internal fun heartShouldPulse(
    seen: Boolean,
    previousTrackId: Long?,
    previousFavourite: Boolean,
    trackId: Long?,
    isFavourite: Boolean,
): Boolean = seen && previousTrackId == trackId && previousFavourite != isFavourite

private class HeartMemory {
    var seen = false
    var trackId: Long? = null
    var isFavourite = false
}

private const val HEART_PULSE_SCALE = 1.08f
private const val HEART_PULSE_UP_MS = 90
private const val HEART_PULSE_DOWN_MS = 150
