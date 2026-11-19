package com.sonara.app.data.media

import com.sonara.app.data.local.database.TrackEntity

/** Raw values as MediaStore reports them, before any clean-up. Strings may be null, blank or "<unknown>". */
data class RawAudioRow(
    val id: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val albumId: Long,
    val genre: String?,
    val durationMs: Long,
    /** MediaStore packs the disc number into the track column: 2005 means disc 2, track 5. */
    val rawTrack: Int,
    val year: Int,
    val dateAdded: Long,
    val dateModified: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val relativePath: String?,
    val displayName: String?,
)

/** Pure clean-up rules turning a MediaStore row into a [TrackEntity]. */
object TrackNormalizer {

    const val UNKNOWN_ARTIST = "Unknown artist"
    const val UNKNOWN_ALBUM = "Unknown album"

    /** MediaStore writes this literal for missing artist/album tags. */
    private const val MEDIASTORE_UNKNOWN = "<unknown>"

    fun normalize(row: RawAudioRow): TrackEntity {
        val displayName = row.displayName?.trim().orEmpty().ifEmpty { "Track ${row.id}" }
        return TrackEntity(
            id = row.id,
            title = cleanText(row.title) ?: titleFromFileName(displayName),
            artist = cleanText(row.artist) ?: UNKNOWN_ARTIST,
            album = cleanText(row.album) ?: UNKNOWN_ALBUM,
            albumArtist = cleanText(row.albumArtist),
            albumId = row.albumId,
            genre = cleanText(row.genre),
            durationMs = row.durationMs.coerceAtLeast(0L),
            trackNumber = trackNumber(row.rawTrack),
            discNumber = discNumber(row.rawTrack),
            year = row.year.coerceAtLeast(0),
            dateAdded = row.dateAdded,
            dateModified = row.dateModified,
            sizeBytes = row.sizeBytes,
            mimeType = row.mimeType?.takeIf { it.isNotBlank() },
            relativePath = row.relativePath?.takeIf { it.isNotBlank() },
            displayName = displayName,
        )
    }

    /** Trimmed text, or null when the value is missing, blank or MediaStore's "<unknown>" marker. */
    fun cleanText(value: String?): String? {
        val trimmed = value?.trim().orEmpty()
        return if (trimmed.isEmpty() || trimmed.equals(MEDIASTORE_UNKNOWN, ignoreCase = true)) null else trimmed
    }

    fun titleFromFileName(displayName: String): String {
        val dot = displayName.lastIndexOf('.')
        val stem = if (dot > 0) displayName.substring(0, dot) else displayName
        return stem.trim().ifEmpty { displayName }
    }

    fun trackNumber(rawTrack: Int): Int = if (rawTrack > 0) rawTrack % 1000 else 0

    fun discNumber(rawTrack: Int): Int = if (rawTrack >= 1000) rawTrack / 1000 else 0
}
