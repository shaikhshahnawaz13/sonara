package com.sonara.app.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One slot of the persisted playback queue, in playlist order (not shuffled order). */
@Entity(tableName = "playback_queue")
data class PlaybackQueueItemEntity(
    @PrimaryKey val position: Int,
    val trackId: Long,
)

/** The single row (id = 0) describing where playback was. */
@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    /** A `Player.REPEAT_MODE_*` value. */
    val repeatMode: Int,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
