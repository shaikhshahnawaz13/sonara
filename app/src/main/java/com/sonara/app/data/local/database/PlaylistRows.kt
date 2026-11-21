package com.sonara.app.data.local.database

import androidx.room.Embedded

/** A playlist with what the list screen shows, computed against the `tracks` cache. */
data class PlaylistRow(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
    /** Every entry, including those whose audio file is no longer in the library. */
    val entryCount: Int,
    /** Entries whose track exists in the library. */
    val trackCount: Int,
    /** The first available track by position; its artwork represents the playlist. */
    val artworkTrackId: Long?,
    val durationMs: Long,
)

/**
 * One playlist entry joined with its track. [track] is null when the audio file is no longer in the
 * library (deleted, or its storage is gone); the entry stays so the person decides what to do with it.
 */
data class PlaylistEntryRow(
    val entryId: Long,
    val entryTrackId: Long,
    val entryPosition: Int,
    @Embedded val track: TrackEntity?,
)
