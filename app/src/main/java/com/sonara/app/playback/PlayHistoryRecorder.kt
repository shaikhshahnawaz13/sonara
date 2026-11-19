package com.sonara.app.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.sonara.app.data.local.dao.HistoryDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Writes a play-history entry once a track has really been playing for [MIN_PLAY_MS] without interruption.
 * Skipping through a queue therefore records nothing, pausing and resuming counts the track once, and
 * repeat-one counts every loop. Home's "Continue Listening" reads this history.
 */
class PlayHistoryRecorder(
    private val player: Player,
    private val historyDao: HistoryDao,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) : Player.Listener {

    private var itemSerial = 0
    private var recordedSerial = -1
    private var pending: Job? = null

    fun start() = player.addListener(this)

    fun stop() {
        player.removeListener(this)
        pending?.cancel()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        itemSerial++
        pending?.cancel()
        if (player.isPlaying) schedule()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        pending?.cancel()
        if (isPlaying) schedule()
    }

    private fun schedule() {
        val serial = itemSerial
        if (recordedSerial == serial) return
        val trackId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        pending = scope.launch {
            delay(MIN_PLAY_MS)
            recordedSerial = serial
            historyDao.recordPlay(trackId, clock())
        }
    }

    private companion object {
        const val MIN_PLAY_MS = 5_000L
    }
}
