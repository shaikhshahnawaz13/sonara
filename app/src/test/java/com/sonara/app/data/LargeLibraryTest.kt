package com.sonara.app.data

import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.local.database.TrackFingerprint
import com.sonara.app.data.local.preferences.SongSort
import com.sonara.app.data.media.RawAudioRow
import com.sonara.app.data.media.TrackNormalizer
import com.sonara.app.data.model.SearchEngine
import com.sonara.app.data.model.SongSorter
import com.sonara.app.data.repository.LibraryDiffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Library-size checks for 100, 1,000 and 5,000 tracks. These run on the JVM, so they prove the pure logic
 * (search, sorting, scan diff, tag clean-up) stays correct and cheap as the library grows. They say nothing
 * about phone speed, Room query time, artwork memory or scrolling; those need a device.
 *
 * The time limits are deliberately generous (a CI machine may be slow); they exist to catch a quadratic
 * regression, not to measure.
 */
class LargeLibraryTest {

    private val sizes = listOf(100, 1_000, 5_000)

    private fun library(n: Int): List<TrackEntity> = List(n) { i ->
        val id = i + 1L
        TrackEntity(
            id = id,
            title = "Song ${"%05d".format(i)} ${WORDS[i % WORDS.size]}",
            artist = "Artist ${i % 97}",
            album = "Album ${i % 211}",
            albumArtist = null,
            albumId = (i % 211) + 1L,
            genre = if (i % 5 == 0) null else "Genre ${i % 13}",
            durationMs = 180_000L + i,
            trackNumber = i % 20,
            discNumber = 0,
            year = 2000 + i % 25,
            dateAdded = 1_700_000_000L + i,
            dateModified = 1_700_000_000L + i,
            sizeBytes = 4_000_000L + i,
            mimeType = "audio/mpeg",
            relativePath = "Music/Folder ${i % 40}/",
            displayName = "song$i.mp3",
        )
    }

    private inline fun <T> within(limitMs: Long, what: String, block: () -> T): T {
        val start = System.nanoTime()
        val result = block()
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("$what took ${ms}ms (limit ${limitMs}ms)", ms <= limitMs)
        return result
    }

    @Test
    fun searchStaysCorrectAndBoundedAtEverySize() {
        for (n in sizes) {
            val songs = library(n)
            val hit = within(2_000, "search in $n") {
                SearchEngine.search("song 00042", songs, emptyList(), emptyList(), emptyList(), emptyList())
            }
            assertEquals("exact title first at $n", 43L, hit.songs.first().id)
            val broad = within(2_000, "broad search in $n") {
                SearchEngine.search("artist", songs, emptyList(), emptyList(), emptyList(), emptyList())
            }
            assertTrue(broad.songs.size <= SearchEngine.MAX_PER_SECTION)
            assertEquals(n > SearchEngine.MAX_PER_SECTION, broad.truncated)
            val none = SearchEngine.search("zzzz-no-such", songs, emptyList(), emptyList(), emptyList(), emptyList())
            assertTrue(none.isEmpty)
        }
    }

    @Test
    fun everySortKeepsEveryTrackExactlyOnce() {
        for (n in sizes) {
            val songs = library(n)
            for (sort in SongSort.entries) {
                val sorted = within(2_000, "$sort sort of $n") { SongSorter.sort(songs, sort) }
                assertEquals(n, sorted.size)
                assertEquals(n, sorted.map { it.id }.toSet().size)
            }
        }
    }

    @Test
    fun rescanOfAnUnchangedLibraryWritesNothing() {
        for (n in sizes) {
            val songs = library(n)
            val stored = songs.map { TrackFingerprint(it.id, it.dateModified, it.sizeBytes) }
            val diff = within(2_000, "diff of $n") { LibraryDiffer.compute(stored, songs) }
            assertTrue(diff.isEmpty)
            assertEquals(n, diff.unchanged)
        }
    }

    @Test
    fun rescanWritesOnlyWhatChanged() {
        val songs = library(5_000)
        val stored = songs.map { TrackFingerprint(it.id, it.dateModified, it.sizeBytes) }
        // 100 files edited, 50 removed from the device, 25 new.
        val scanned = songs.drop(50).mapIndexed { i, t -> if (i < 100) t.copy(sizeBytes = t.sizeBytes + 1) else t } +
            library(25).map { it.copy(id = it.id + 100_000) }
        val diff = LibraryDiffer.compute(stored, scanned)
        assertEquals(100, diff.updated)
        assertEquals(25, diff.added)
        assertEquals(50, diff.toDeleteIds.size)
        assertEquals(125, diff.toUpsert.size)
    }

    @Test
    fun hostileTagsNeverProduceBlankOrOversizedSurprises() {
        val long = "A".repeat(10_000)
        val rows = listOf(
            RawAudioRow(1, null, null, null, null, 0, null, 1000, 0, 0, 0, 0, 0, null, null, "No Tags.mp3"),
            RawAudioRow(2, "   ", "\u0000", "", "", 0, "", 1000, 2005, 0, 0, 0, 0, null, null, "blank.flac"),
            RawAudioRow(3, long, long, long, long, 5, long, 1000, 7, 1999, 1, 1, 1, "audio/mpeg", "Music/$long/", "$long.mp3"),
            RawAudioRow(4, "Émile – 日本語 🎵", "Ünïcode", "アルバム", null, 6, null, 1000, 1, 2020, 1, 1, 1, null, null, "u.mp3"),
        )
        rows.forEach {
            val t = TrackNormalizer.normalize(it)
            assertTrue("title of ${it.id}", t.title.isNotBlank())
            assertTrue("artist of ${it.id}", t.artist.isNotBlank())
            assertTrue("album of ${it.id}", t.album.isNotBlank())
        }
        assertEquals("No Tags", TrackNormalizer.normalize(rows[0]).title)
        val packed = TrackNormalizer.normalize(rows[1])
        assertEquals(2, packed.discNumber)
        assertEquals(5, packed.trackNumber)
        assertEquals("blank", packed.title)
    }

    private companion object {
        val WORDS = listOf("alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel")
    }
}
