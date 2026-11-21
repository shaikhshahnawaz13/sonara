package com.sonara.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * The app's handle on playback. It is a MediaController client of [PlaybackService]: it holds no player
 * and no playback state of its own, it only mirrors the service's state for the UI and forwards commands.
 * It is owned by the application container, not by any screen.
 *
 * All methods must be called on the main thread.
 */
class PlaybackController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PlaybackUiState())
    private val _positionMs = MutableStateFlow(0L)

    val state: StateFlow<PlaybackUiState> = _state
    val positionMs: StateFlow<Long> = _positionMs

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())

    /** The queue in playback-list order. Updated only when the list itself changes. */
    val queue: StateFlow<List<QueueEntry>> = _queue

    private var pendingController: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED) || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                refreshQueue(player)
            }
            refresh(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(lastError = error.errorCodeName)
        }
    }

    /** Connects to the playback service (starting it if needed). Safe to call repeatedly. */
    fun connect() {
        if (controller != null || pendingController != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        pendingController = future
        future.addListener(
            {
                // Ignore a result that arrived after disconnect() was called.
                if (pendingController !== future) return@addListener
                pendingController = null
                try {
                    val connected = future.get()
                    controller = connected
                    connected.addListener(listener)
                    refreshQueue(connected)
                    refresh(connected)
                    startPositionUpdates()
                } catch (e: Exception) {
                    _state.value = PlaybackUiState(lastError = e.message ?: "Could not connect to playback")
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    /** Releases the controller. Playback itself continues in the service. */
    fun disconnect() {
        positionJob?.cancel()
        positionJob = null
        pendingController?.let { MediaController.releaseFuture(it) }
        pendingController = null
        controller?.let {
            it.removeListener(listener)
            it.release()
        }
        controller = null
        _state.value = PlaybackUiState()
        _queue.value = emptyList()
        _positionMs.value = 0L
    }

    /** Replaces the queue with [trackIds] and starts playing at [startIndex]. */
    fun playTracks(trackIds: List<Long>, startIndex: Int): Boolean {
        val c = controller ?: return false
        if (trackIds.isEmpty()) return false
        c.setMediaItems(trackIds.map(::trackRequestItem), startIndex.coerceIn(0, trackIds.lastIndex), 0L)
        c.prepare()
        c.play()
        return true
    }

    /** Turns shuffle on and starts playing [trackIds] from a random track. */
    fun playShuffled(trackIds: List<Long>): Boolean {
        val c = controller ?: return false
        if (trackIds.isEmpty()) return false
        c.shuffleModeEnabled = true
        c.setMediaItems(trackIds.map(::trackRequestItem), Random.nextInt(trackIds.size), 0L)
        c.prepare()
        c.play()
        return true
    }

    /** Inserts [trackIds] after the current track. With an empty queue they simply start playing. */
    fun playNext(trackIds: List<Long>): Boolean {
        val c = controller ?: return false
        if (trackIds.isEmpty()) return false
        if (c.mediaItemCount == 0) {
            playTracks(trackIds, 0)
        } else {
            c.addMediaItems(c.currentMediaItemIndex + 1, trackIds.map(::trackRequestItem))
        }
        return true
    }

    /** Appends [trackIds] to the end of the queue. With an empty queue they simply start playing. */
    fun addToQueue(trackIds: List<Long>): Boolean {
        val c = controller ?: return false
        if (trackIds.isEmpty()) return false
        if (c.mediaItemCount == 0) {
            playTracks(trackIds, 0)
        } else {
            c.addMediaItems(trackIds.map(::trackRequestItem))
        }
        return true
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying || c.playWhenReady && c.playbackState == Player.STATE_BUFFERING) c.pause() else c.play()
    }

    /** Jumps to the [index]th queue entry and plays it. Out-of-range indices are ignored. */
    fun skipToQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0L)
        c.play()
    }

    /** Removes the [index]th queue entry. Removing the playing one moves playback on to the next. */
    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.removeMediaItem(index)
    }

    fun skipToNext() { controller?.seekToNextMediaItem() }

    /** Restarts the track if it has played for a few seconds, otherwise goes to the previous one. */
    fun skipToPrevious() { controller?.seekToPrevious() }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        _positionMs.value = positionMs.coerceAtLeast(0L)
    }

    fun setShuffle(enabled: Boolean) { controller?.shuffleModeEnabled = enabled }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = RepeatSetting.fromPlayerValue(c.repeatMode).next().playerValue
    }

    fun clearError() { _state.value = _state.value.copy(lastError = null) }

    private fun refresh(player: Player) {
        val item = player.currentMediaItem
        val meta = item?.mediaMetadata
        val duration = player.duration
        _state.value = _state.value.copy(
            connected = true,
            currentTrackId = item?.mediaId?.toLongOrNull(),
            title = meta?.title?.toString(),
            artist = meta?.artist?.toString(),
            album = meta?.albumTitle?.toString(),
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            durationMs = if (duration == C.TIME_UNSET) 0L else duration.coerceAtLeast(0L),
            shuffleEnabled = player.shuffleModeEnabled,
            repeat = RepeatSetting.fromPlayerValue(player.repeatMode),
            queueSize = player.mediaItemCount,
            queueIndex = if (item == null) -1 else player.currentMediaItemIndex,
            playWhenReady = player.playWhenReady,
            hasPrevious = player.hasPreviousMediaItem(),
            hasNext = player.hasNextMediaItem(),
        )
        _positionMs.value = player.currentPosition.coerceAtLeast(0L)
    }

    private fun refreshQueue(player: Player) {
        _queue.value = List(player.mediaItemCount) { i ->
            val item = player.getMediaItemAt(i)
            val meta = item.mediaMetadata
            QueueEntry(
                trackId = item.mediaId.toLongOrNull(),
                title = meta.title?.toString() ?: "Unknown title",
                artist = meta.artist?.toString() ?: "Unknown artist",
            )
        }
    }

    /** Polls the position while connected; cheap, and only the position flow changes. */
    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                val c = controller
                if (c != null && c.isPlaying) _positionMs.value = c.currentPosition.coerceAtLeast(0L)
                delay(POSITION_POLL_MS)
            }
        }
    }

    private companion object {
        const val POSITION_POLL_MS = 250L
    }
}
