package com.sonara.app.data.local.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One audio file discovered through MediaStore. [id] is the MediaStore `_ID`, so playlists, favourites
 * and history rows that already reference tracks by id stay valid. This table is a cache of MediaStore:
 * it is rebuilt from it incrementally and never holds anything the user created.
 *
 * Text fields are already normalised (see TrackNormalizer): they are never blank.
 */
@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["dateAdded"]),
    ],
)
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String?,
    val albumId: Long,
    val genre: String?,
    val durationMs: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    /** Seconds since epoch, as MediaStore reports it. */
    val dateAdded: Long,
    /** Seconds since epoch; together with [sizeBytes] this detects a changed file. */
    val dateModified: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val relativePath: String?,
    val displayName: String,
)

/** The columns needed to decide whether a stored track still matches MediaStore. */
data class TrackFingerprint(
    val id: Long,
    val dateModified: Long,
    val sizeBytes: Long,
)
