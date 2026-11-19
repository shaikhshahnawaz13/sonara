package com.sonara.app.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps the persisted queue and position in step with a [Player]. Everything runs on the player's
 * (main) thread; the Room writes themselves are suspend functions.
 *
 * - The queue is rewritten only when the playlist changes (debounced).
 * - The position/shuffle/repeat row is written on pause, track change, seek, mode change, every
 *   [POSITION_SAVE_INTERVAL_MS] while playing, and from [saveNow] when the service shuts down.
 */
class PlaybackPersistence(
    private val player: Player,
    private val store: PlaybackStore,
    private val scope: CoroutineScope,
) : Player.Listener {

    private var queueJob: Job? = null
    private var tickJob: Job? = null

    /** Set while the service is putting the saved queue back, so that does not overwrite what was saved. */
    var suspended: Boolean = false

    fun start() {
        player.addListener(this)
        tickJob = scope.launch {
            while (true) {
                delay(POSITION_SAVE_INTERVAL_MS)
                if (player.isPlaying) saveState()
            }
        }
    }

    fun stop() {
        player.removeListener(this)
        queueJob?.cancel()
        tickJob?.cancel()
    }

    /**
     * Reads the player right now and writes queue and state in [writeScope]. Used when the service is being
     * torn down: the snapshot is taken before the player is released, the write may finish afterwards,
     * so [writeScope] must outlive the service.
     */
    fun flush(writeScope: CoroutineScope) {
        if (suspended) return
        val ids = currentQueueIds()
        val index = player.currentMediaItemIndex
        val position = player.currentPosition
        val shuffle = player.shuffleModeEnabled
        val repeat = player.repeatMode
        writeScope.launch {
            store.saveQueue(ids)
            if (ids.isNotEmpty()) store.saveState(index, position, shuffle, repeat)
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (suspended) return
        queueJob?.cancel()
        queueJob = scope.launch {
            delay(QUEUE_SAVE_DEBOUNCE_MS)
            val ids = currentQueueIds()
            store.saveQueue(ids)
            writeState()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = saveState()

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) saveState()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = saveState()

    override fun onRepeatModeChanged(repeatMode: Int) = saveState()

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) saveState()
    }

    private fun saveState() {
        if (suspended) return
        // Read the player now (main thread); write in the background.
        val index = player.currentMediaItemIndex
        val position = player.currentPosition
        val shuffle = player.shuffleModeEnabled
        val repeat = player.repeatMode
        if (player.mediaItemCount == 0) return
        scope.launch { store.saveState(index, position, shuffle, repeat) }
    }

    private suspend fun writeState() {
        if (player.mediaItemCount == 0) return
        store.saveState(
            player.currentMediaItemIndex,
            player.currentPosition,
            player.shuffleModeEnabled,
            player.repeatMode,
        )
    }

    private fun currentQueueIds(): List<Long> =
        List(player.mediaItemCount) { i -> player.getMediaItemAt(i).mediaId.toLongOrNull() ?: -1L }
            .filter { it >= 0 }

    private companion object {
        const val POSITION_SAVE_INTERVAL_MS = 5_000L
        const val QUEUE_SAVE_DEBOUNCE_MS = 500L
    }
}
