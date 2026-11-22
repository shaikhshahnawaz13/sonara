package com.sonara.app.data.repository

import com.sonara.app.data.local.database.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderExclusionsTest {

    private fun track(id: Long, path: String?) = TrackEntity(
        id = id, title = "t$id", artist = "a", album = "b", albumArtist = null, albumId = 1, genre = null,
        durationMs = 1000, trackNumber = 1, discNumber = 1, year = 2000, dateAdded = 1, dateModified = 1,
        sizeBytes = 1, mimeType = null, relativePath = path, displayName = "t$id.mp3",
    )

    @Test
    fun normalizeMakesOneCanonicalForm() {
        assertEquals("Music/Podcasts/", FolderExclusions.normalize("/Music//Podcasts"))
        assertEquals("Music/Podcasts/", FolderExclusions.normalize("Music\\Podcasts\\"))
        assertEquals("Music/", FolderExclusions.normalize(" Music/ "))
        assertNull(FolderExclusions.normalize(""))
        assertNull(FolderExclusions.normalize(" / "))
    }

    @Test
    fun excludingAFolderExcludesItsSubfolders() {
        val excluded = setOf("Music/Podcasts/")
        assertTrue(FolderExclusions.isExcluded("Music/Podcasts/", excluded))
        assertTrue(FolderExclusions.isExcluded("Music/Podcasts/2024/", excluded))
        assertFalse(FolderExclusions.isExcluded("Music/Rock/", excluded))
    }

    @Test
    fun aSiblingWithTheSamePrefixIsNotExcluded() {
        assertFalse(FolderExclusions.isExcluded("Music/Podcasts Extra/", setOf("Music/Podcasts/")))
    }

    @Test
    fun unknownFolderIsNeverExcluded() {
        assertFalse(FolderExclusions.isExcluded(null, setOf("Music/")))
        assertFalse(FolderExclusions.isExcluded("Music/", emptySet()))
    }

    @Test
    fun filterKeepsOrderAndReturnsSameListWhenNothingIsExcluded() {
        val tracks = listOf(track(1, "Music/A/"), track(2, "Podcasts/"), track(3, null), track(4, "Music/B/"))
        assertSame(tracks, FolderExclusions.filter(tracks, emptySet()))
        assertEquals(listOf(1L, 3L, 4L), FolderExclusions.filter(tracks, setOf("Podcasts/")).map { it.id })
    }

    @Test
    fun excludingEverythingIsAllowed() {
        val tracks = listOf(track(1, "Music/A/"), track(2, "Music/B/"))
        assertTrue(FolderExclusions.filter(tracks, setOf("Music/")).isEmpty())
    }

    @Test
    fun stampChangesWithTheSetAndIgnoresOrder() {
        assertEquals("", FolderExclusions.stamp(emptySet()))
        assertEquals(FolderExclusions.stamp(setOf("A/", "B/")), FolderExclusions.stamp(linkedSetOf("B/", "A/")))
        assertFalse(FolderExclusions.stamp(setOf("A/")) == FolderExclusions.stamp(setOf("A/", "B/")))
    }

    @Test
    fun displayNameIsTheLastSegment() {
        assertEquals("Podcasts", FolderExclusions.displayName("Music/Podcasts/"))
        assertEquals("Music", FolderExclusions.displayName("Music/"))
    }

    @Test
    fun excludedSongsAreOnlyEverRemovedFromTheTracksCacheListNotFromAnythingElse() {
        // The diff a scan applies is computed from the filtered list: an excluded track leaves only `tracks`.
        val stored = listOf(track(1, "Music/A/"), track(2, "Podcasts/")).map {
            com.sonara.app.data.local.database.TrackFingerprint(it.id, it.dateModified, it.sizeBytes)
        }
        val scanned = FolderExclusions.filter(listOf(track(1, "Music/A/"), track(2, "Podcasts/")), setOf("Podcasts/"))
        val diff = LibraryDiffer.compute(stored, scanned)
        assertEquals(listOf(2L), diff.toDeleteIds)
        assertTrue(diff.toUpsert.isEmpty())
    }

    @Test
    fun includingAgainBringsTheTrackBackWithTheSameId() {
        val all = listOf(track(1, "Music/A/"), track(2, "Podcasts/"))
        val stored = FolderExclusions.filter(all, setOf("Podcasts/")).map {
            com.sonara.app.data.local.database.TrackFingerprint(it.id, it.dateModified, it.sizeBytes)
        }
        val diff = LibraryDiffer.compute(stored, FolderExclusions.filter(all, emptySet()))
        assertEquals(listOf(2L), diff.toUpsert.map { it.id })
        assertTrue(diff.toDeleteIds.isEmpty())
    }
}
