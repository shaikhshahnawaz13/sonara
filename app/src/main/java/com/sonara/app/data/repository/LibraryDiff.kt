package com.sonara.app.data.repository

import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.local.database.TrackFingerprint

/** What a scan changes in the stored library. */
data class LibraryDiff(
    val toUpsert: List<TrackEntity>,
    val toDeleteIds: List<Long>,
    val added: Int,
    val updated: Int,
    val unchanged: Int,
) {
    val isEmpty: Boolean get() = toUpsert.isEmpty() && toDeleteIds.isEmpty()
}

/** Pure comparison of the stored library with a fresh scan. */
object LibraryDiffer {

    fun compute(stored: List<TrackFingerprint>, scanned: List<TrackEntity>): LibraryDiff {
        val storedById = stored.associateBy { it.id }
        val scannedIds = HashSet<Long>(scanned.size * 2)
        val upsert = ArrayList<TrackEntity>()
        var added = 0
        var updated = 0
        var unchanged = 0

        for (track in scanned) {
            scannedIds += track.id
            val existing = storedById[track.id]
            when {
                existing == null -> { upsert += track; added++ }
                existing.dateModified != track.dateModified || existing.sizeBytes != track.sizeBytes -> {
                    upsert += track; updated++
                }
                else -> unchanged++
            }
        }
        val deleted = stored.asSequence().map { it.id }.filter { it !in scannedIds }.toList()
        return LibraryDiff(upsert, deleted, added, updated, unchanged)
    }
}
