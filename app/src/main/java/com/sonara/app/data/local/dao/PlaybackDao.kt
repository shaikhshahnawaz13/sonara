package com.sonara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.sonara.app.data.local.database.PlaybackQueueItemEntity
import com.sonara.app.data.local.database.PlaybackStateEntity

@Dao
abstract class PlaybackDao {

    @Query("SELECT trackId FROM playback_queue ORDER BY position ASC")
    abstract suspend fun queueTrackIds(): List<Long>

    @Query("DELETE FROM playback_queue")
    abstract suspend fun clearQueue()

    @Insert
    abstract suspend fun insertQueue(items: List<PlaybackQueueItemEntity>)

    @Transaction
    open suspend fun replaceQueue(trackIds: List<Long>) {
        clearQueue()
        if (trackIds.isNotEmpty()) {
            insertQueue(trackIds.mapIndexed { index, id -> PlaybackQueueItemEntity(index, id) })
        }
    }

    @Query("SELECT * FROM playback_state WHERE id = 0")
    abstract suspend fun state(): PlaybackStateEntity?

    @Upsert
    abstract suspend fun upsertState(state: PlaybackStateEntity)
}
