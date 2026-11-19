package com.sonara.app.data.repository

import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.local.database.TrackFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryDifferTest {

    private fun track(id: Long, modified: Long = 10, size: Long = 100) = TrackEntity(
        id = id, title = "t$id", artist = "a", album = "b", albumArtist = null, albumId = 1, genre = null,
        durationMs = 1000, trackNumber = 0, discNumber = 0, year = 0, dateAdded = 1, dateModified = modified,
        sizeBytes = size, mimeType = null, relativePath = null, displayName = "t$id.mp3",
    )

    private fun fp(id: Long, modified: Long = 10, size: Long = 100) = TrackFingerprint(id, modified, size)

    @Test
    fun identicalLibraryIsEmptyDiff() {
        val diff = LibraryDiffer.compute(listOf(fp(1), fp(2)), listOf(track(1), track(2)))
        assertTrue(diff.isEmpty)
        assertEquals(2, diff.unchanged)
    }

    @Test
    fun newTracksAreAdded() {
        val diff = LibraryDiffer.compute(listOf(fp(1)), listOf(track(1), track(2)))
        assertEquals(listOf(2L), diff.toUpsert.map { it.id })
        assertEquals(1, diff.added)
        assertEquals(1, diff.unchanged)
    }

    @Test
    fun changedModifiedTimeOrSizeIsUpdated() {
        val diff = LibraryDiffer.compute(
            listOf(fp(1), fp(2), fp(3)),
            listOf(track(1, modified = 11), track(2, size = 101), track(3)),
        )
        assertEquals(listOf(1L, 2L), diff.toUpsert.map { it.id })
        assertEquals(2, diff.updated)
        assertEquals(0, diff.added)
    }

    @Test
    fun missingTracksAreRemoved() {
        val diff = LibraryDiffer.compute(listOf(fp(1), fp(2), fp(3)), listOf(track(2)))
        assertEquals(listOf(1L, 3L), diff.toDeleteIds)
        assertTrue(diff.toUpsert.isEmpty())
    }

    @Test
    fun emptyScanRemovesEverythingAndEmptyStoreAddsEverything() {
        assertEquals(listOf(1L, 2L), LibraryDiffer.compute(listOf(fp(1), fp(2)), emptyList()).toDeleteIds)
        assertEquals(2, LibraryDiffer.compute(emptyList(), listOf(track(1), track(2))).added)
    }
}
