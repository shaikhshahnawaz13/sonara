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

    private var pendingController: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
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
        _positionMs.value = 0L
    }

    /** Replaces the queue with [trackIds] and starts playing at [startIndex]. */
    fun playTracks(trackIds: List<Long>, startIndex: Int) {
        val c = controller ?: return
        if (trackIds.isEmpty()) return
        c.setMediaItems(trackIds.map(::trackRequestItem), startIndex.coerceIn(0, trackIds.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying || c.playWhenReady && c.playbackState == Player.STATE_BUFFERING) c.pause() else c.play()
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
            hasPrevious = player.hasPreviousMediaItem(),
            hasNext = player.hasNextMediaItem(),
        )
        _positionMs.value = player.currentPosition.coerceAtLeast(0L)
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
