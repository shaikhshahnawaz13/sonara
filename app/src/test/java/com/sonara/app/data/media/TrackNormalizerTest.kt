package com.sonara.app.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackNormalizerTest {

    private fun row(
        title: String? = "Song",
        artist: String? = "Artist",
        album: String? = "Album",
        albumArtist: String? = null,
        genre: String? = null,
        rawTrack: Int = 0,
        displayName: String? = "song.mp3",
        durationMs: Long = 1000,
    ) = RawAudioRow(
        id = 42, title = title, artist = artist, album = album, albumArtist = albumArtist, albumId = 7,
        genre = genre, durationMs = durationMs, rawTrack = rawTrack, year = 2020, dateAdded = 1, dateModified = 2,
        sizeBytes = 3, mimeType = "audio/mpeg", relativePath = "Music/", displayName = displayName,
    )

    @Test
    fun keepsRealTagsTrimmed() {
        val t = TrackNormalizer.normalize(row(title = "  Hello ", artist = " A ", album = " B "))
        assertEquals("Hello", t.title)
        assertEquals("A", t.artist)
        assertEquals("B", t.album)
    }

    @Test
    fun mediaStoreUnknownMarkerBecomesUnknownLabel() {
        val t = TrackNormalizer.normalize(row(artist = "<unknown>", album = "<UNKNOWN>"))
        assertEquals(TrackNormalizer.UNKNOWN_ARTIST, t.artist)
        assertEquals(TrackNormalizer.UNKNOWN_ALBUM, t.album)
    }

    @Test
    fun blankOrNullTagsFallBack() {
        val t = TrackNormalizer.normalize(row(artist = "   ", album = null, albumArtist = "", genre = " "))
        assertEquals(TrackNormalizer.UNKNOWN_ARTIST, t.artist)
        assertEquals(TrackNormalizer.UNKNOWN_ALBUM, t.album)
        assertNull(t.albumArtist)
        assertNull(t.genre)
    }

    @Test
    fun missingTitleUsesFileNameWithoutExtension() {
        assertEquals("My Track", TrackNormalizer.normalize(row(title = null, displayName = "My Track.flac")).title)
        assertEquals("Track.v2", TrackNormalizer.normalize(row(title = " ", displayName = "Track.v2.mp3")).title)
    }

    @Test
    fun fileNameWithoutExtensionOrWithOnlyADotIsKept() {
        assertEquals("noext", TrackNormalizer.titleFromFileName("noext"))
        assertEquals(".hidden", TrackNormalizer.titleFromFileName(".hidden"))
    }

    @Test
    fun missingDisplayNameStillProducesATitle() {
        val t = TrackNormalizer.normalize(row(title = null, displayName = null))
        assertEquals("Track 42", t.title)
        assertEquals("Track 42", t.displayName)
    }

    @Test
    fun discAndTrackAreUnpackedFromTheCombinedColumn() {
        assertEquals(5, TrackNormalizer.trackNumber(5))
        assertEquals(0, TrackNormalizer.discNumber(5))
        assertEquals(5, TrackNormalizer.trackNumber(2005))
        assertEquals(2, TrackNormalizer.discNumber(2005))
        assertEquals(0, TrackNormalizer.trackNumber(0))
        assertEquals(0, TrackNormalizer.trackNumber(-3))
    }

    @Test
    fun negativeDurationIsClamped() {
        assertEquals(0, TrackNormalizer.normalize(row(durationMs = -9)).durationMs)
    }
}
