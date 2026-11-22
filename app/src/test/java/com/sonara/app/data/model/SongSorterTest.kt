package com.sonara.app.data.model

import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.local.preferences.SongSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SongSorterTest {

    private fun t(id: Long, title: String, artist: String = "A", album: String = "X", disc: Int = 1, no: Int = 1, added: Long = 0) =
        TrackEntity(
            id = id, title = title, artist = artist, album = album, albumArtist = null, albumId = 1, genre = null,
            durationMs = 1000, trackNumber = no, discNumber = disc, year = 0, dateAdded = added, dateModified = 0,
            sizeBytes = 0, mimeType = null, relativePath = null, displayName = "$id.mp3",
        )

    @Test
    fun titleKeepsTheDatabaseOrderUntouched() {
        val list = listOf(t(2, "b"), t(1, "a"))
        assertSame(list, SongSorter.sort(list, SongSort.Title))
    }

    @Test
    fun artistThenAlbumThenTrackNumber() {
        val list = listOf(
            t(1, "z", artist = "beta", album = "q", no = 1),
            t(2, "y", artist = "Alpha", album = "r", no = 2),
            t(3, "x", artist = "alpha", album = "r", no = 1),
        )
        assertEquals(listOf(3L, 2L, 1L), SongSorter.sort(list, SongSort.Artist).map { it.id })
    }

    @Test
    fun albumOrdersByDiscThenTrack() {
        val list = listOf(
            t(1, "c", album = "Disc", disc = 2, no = 1),
            t(2, "b", album = "Disc", disc = 1, no = 2),
            t(3, "a", album = "Disc", disc = 1, no = 1),
            t(4, "d", album = "Alpha"),
        )
        assertEquals(listOf(4L, 3L, 2L, 1L), SongSorter.sort(list, SongSort.Album).map { it.id })
    }

    @Test
    fun newestFirstWithIdAsTieBreak() {
        val list = listOf(t(1, "a", added = 10), t(2, "b", added = 30), t(3, "c", added = 30))
        assertEquals(listOf(3L, 2L, 1L), SongSorter.sort(list, SongSort.RecentlyAdded).map { it.id })
    }

    @Test
    fun sortingNeverDropsOrDuplicatesSongs() {
        val list = (1L..50L).map { t(it, "t${it % 7}", artist = "a${it % 3}", added = it % 5) }
        SongSort.entries.forEach { s ->
            assertEquals(list.map { it.id }.sorted(), SongSorter.sort(list, s).map { it.id }.sorted())
        }
    }
}
