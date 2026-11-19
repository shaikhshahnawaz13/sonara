package com.sonara.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sonara.app.data.local.dao.FavouriteDao
import com.sonara.app.data.local.dao.HistoryDao
import com.sonara.app.data.local.dao.PlaybackDao
import com.sonara.app.data.local.dao.PlaylistDao
import com.sonara.app.data.local.dao.SelectedFolderDao
import com.sonara.app.data.local.dao.TrackDao

/**
 * Schema version 2 (v1 -> v2 adds tracks, playback_queue, playback_state). Schemas are exported to app/schemas. There is deliberately no destructive
 * migration fallback: every future schema change must ship an explicit Migration (spec section 8).
 */
@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FavouriteEntity::class,
        PlayHistoryEntity::class,
        SelectedFolderEntity::class,
        TrackEntity::class,
        PlaybackQueueItemEntity::class,
        PlaybackStateEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class SonaraDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun selectedFolderDao(): SelectedFolderDao
    abstract fun trackDao(): TrackDao
    abstract fun playbackDao(): PlaybackDao

    companion object {
        const val NAME = "sonara.db"

        fun create(context: Context): SonaraDatabase =
            Room.databaseBuilder(context.applicationContext, SonaraDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
