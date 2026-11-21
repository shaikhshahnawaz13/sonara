package com.sonara.app.data.model

import com.sonara.app.data.local.database.PlaylistEntryRow
import com.sonara.app.data.local.database.PlaylistRow
import com.sonara.app.data.local.database.TrackEntity

/** A playlist as the UI shows it. Counts cover only tracks that still exist in the library. */
data class PlaylistSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val trackCount: Int,
    /** Entries whose audio file is missing from the library. */
    val unavailableCount: Int,
    val durationMs: Long,
    /** A track whose artwork represents the playlist; null when the playlist has no available track. */
    val artworkTrackId: Long?,
)

/** One position in a playlist. [track] is null when the audio file is no longer in the library. */
data class PlaylistEntry(
    val entryId: Long,
    val trackId: Long,
    val track: TrackEntity?,
) {
    val isAvailable: Boolean get() = track != null
}

fun PlaylistRow.toSummary() = PlaylistSummary(
    id = id,
    name = name,
    description = description,
    trackCount = trackCount,
    unavailableCount = (entryCount - trackCount).coerceAtLeast(0),
    durationMs = durationMs,
    artworkTrackId = artworkTrackId,
)

fun PlaylistEntryRow.toEntry() = PlaylistEntry(entryId = entryId, trackId = entryTrackId, track = track)

enum class PlaylistNameError(val message: String) {
    Empty("GIVE THE PLAYLIST A NAME"),
    TooLong("NAMES CAN BE UP TO ${PlaylistRules.MAX_NAME_LENGTH} CHARACTERS"),
    Duplicate("A PLAYLIST WITH THIS NAME ALREADY EXISTS"),
}

/** Rules that do not need a database, so they can be tested directly. */
object PlaylistRules {
    const val MAX_NAME_LENGTH = 60

    /** Trims and collapses runs of whitespace, so "  My   mix " and "My mix" are the same name. */
    fun normalizeName(raw: String): String = raw.trim().replace(WHITESPACE, " ")

    /**
     * Null when [raw] is a usable name. Names are compared ignoring case. [current] is the name being
     * renamed, which may be kept or only re-cased.
     */
    fun validateName(raw: String, existing: Collection<String>, current: String? = null): PlaylistNameError? {
        val name = normalizeName(raw)
        if (name.isEmpty()) return PlaylistNameError.Empty
        if (name.length > MAX_NAME_LENGTH) return PlaylistNameError.TooLong
        val clash = existing.any { it.equals(name, ignoreCase = true) && !(current != null && it.equals(current, ignoreCase = true)) }
        return if (clash) PlaylistNameError.Duplicate else null
    }

    /** The tracks of [incoming] that may be appended: no repeats, and none already in the playlist. */
    fun planAppend(existing: Collection<Long>, incoming: List<Long>): List<Long> {
        val seen = HashSet<Long>(existing)
        return incoming.filter { seen.add(it) }
    }

    /** A copy of [list] with the item at [from] moved so that it ends at index [to]. Out-of-range moves change nothing. */
    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        val copy = list.toMutableList()
        copy.add(to, copy.removeAt(from))
        return copy
    }

    private val WHITESPACE = Regex("\\s+")
}
