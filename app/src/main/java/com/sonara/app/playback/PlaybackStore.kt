package com.sonara.app.playback

import com.sonara.app.data.local.dao.PlaybackDao
import com.sonara.app.data.local.dao.TrackDao
import com.sonara.app.data.local.database.PlaybackStateEntity

/** Everything needed to put playback back where it was. */
data class SavedPlayback(
    val trackIds: List<Long>,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: Int,
)

/** Room-backed persistence of the queue and playback position. */
class PlaybackStore(
    private val playbackDao: PlaybackDao,
    private val trackDao: TrackDao,
) {
    suspend fun saveQueue(trackIds: List<Long>) = playbackDao.replaceQueue(trackIds)

    suspend fun saveState(currentIndex: Int, positionMs: Long, shuffleEnabled: Boolean, repeatMode: Int) {
        playbackDao.upsertState(
            PlaybackStateEntity(
                currentIndex = currentIndex,
                positionMs = positionMs,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
            ),
        )
    }

    /** The saved playback with deleted tracks removed, or null when there is nothing to restore. */
    suspend fun load(): Pair<RestoredQueue, PlaybackStateEntity>? {
        val state = playbackDao.state() ?: return null
        val saved = playbackDao.queueTrackIds()
        if (saved.isEmpty()) return null
        val available = trackDao.allIds().toHashSet()
        val restored = QueueRestore.resolve(saved, available, state.currentIndex, state.positionMs) ?: return null
        return restored to state
    }
}
