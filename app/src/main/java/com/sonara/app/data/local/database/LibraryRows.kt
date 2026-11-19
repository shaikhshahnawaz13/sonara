package com.sonara.app.data.local.database

/** Query results grouping the `tracks` cache. Plain rows; display rules live in data/model. */

data class AlbumRow(
    val albumId: Long,
    val album: String,
    val artist: String,
    val artistCount: Int,
    val albumArtist: String?,
    val trackCount: Int,
    val durationMs: Long,
    val year: Int,
    /** A track of the album whose artwork represents it. */
    val artworkTrackId: Long,
)

data class ArtistRow(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val artworkTrackId: Long,
)

data class GenreRow(
    val name: String,
    val trackCount: Int,
)

data class FolderRow(
    val path: String,
    val trackCount: Int,
    val durationMs: Long,
)
