package com.sonara.app.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 1 -> 2 (M2): adds the track cache and the persisted playback queue/state.
 * The SQL mirrors what Room generates for the entities; the exported schema JSON in app/schemas is
 * the reference to compare against.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tracks` (" +
                "`id` INTEGER NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`artist` TEXT NOT NULL, " +
                "`album` TEXT NOT NULL, " +
                "`albumArtist` TEXT, " +
                "`albumId` INTEGER NOT NULL, " +
                "`genre` TEXT, " +
                "`durationMs` INTEGER NOT NULL, " +
                "`trackNumber` INTEGER NOT NULL, " +
                "`discNumber` INTEGER NOT NULL, " +
                "`year` INTEGER NOT NULL, " +
                "`dateAdded` INTEGER NOT NULL, " +
                "`dateModified` INTEGER NOT NULL, " +
                "`sizeBytes` INTEGER NOT NULL, " +
                "`mimeType` TEXT, " +
                "`relativePath` TEXT, " +
                "`displayName` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_title` ON `tracks` (`title`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_artist` ON `tracks` (`artist`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_album` ON `tracks` (`album`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_dateAdded` ON `tracks` (`dateAdded`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playback_queue` (" +
                "`position` INTEGER NOT NULL, `trackId` INTEGER NOT NULL, PRIMARY KEY(`position`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playback_state` (" +
                "`id` INTEGER NOT NULL, " +
                "`currentIndex` INTEGER NOT NULL, " +
                "`positionMs` INTEGER NOT NULL, " +
                "`shuffleEnabled` INTEGER NOT NULL, " +
                "`repeatMode` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
    }
}
