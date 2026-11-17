package com.sonara.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sonara.app.data.local.dao.FavouriteDao
import com.sonara.app.data.local.dao.HistoryDao
import com.sonara.app.data.local.dao.PlaylistDao
import com.sonara.app.data.local.dao.SelectedFolderDao

/**
 * Schema version 1. Schemas are exported to app/schemas. There is deliberately no destructive
 * migration fallback: every future schema change must ship an explicit Migration (spec section 8).
 */
@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FavouriteEntity::class,
        PlayHistoryEntity::class,
        SelectedFolderEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SonaraDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun selectedFolderDao(): SelectedFolderDao

    companion object {
        const val NAME = "sonara.db"

        fun create(context: Context): SonaraDatabase =
            Room.databaseBuilder(context.applicationContext, SonaraDatabase::class.java, NAME).build()
    }
}
