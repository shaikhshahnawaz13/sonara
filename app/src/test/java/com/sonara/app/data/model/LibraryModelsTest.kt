package com.sonara.app.data.model

import com.sonara.app.data.local.database.AlbumRow
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryModelsTest {

    private fun row(artist: String = "Solo", artistCount: Int = 1, albumArtist: String? = null) = AlbumRow(
        albumId = 7, album = "Album", artist = artist, artistCount = artistCount, albumArtist = albumArtist,
        trackCount = 3, durationMs = 9000, year = 2001, artworkTrackId = 42,
    )

    @Test
    fun albumArtistWinsWhenPresent() {
        assertEquals("The Band", row(artistCount = 3, albumArtist = "The Band").toAlbum().artist)
    }

    @Test
    fun singleArtistAlbumShowsThatArtist() {
        assertEquals("Solo", row().toAlbum().artist)
    }

    @Test
    fun compilationWithoutAlbumArtistIsVariousArtists() {
        assertEquals(VARIOUS_ARTISTS, row(artistCount = 4).toAlbum().artist)
    }

    @Test
    fun blankAlbumArtistIsIgnored() {
        assertEquals("Solo", row(albumArtist = "  ").toAlbum().artist)
    }

    @Test
    fun albumKeepsIdsAndTotals() {
        val album = row().toAlbum()
        assertEquals(7L, album.id)
        assertEquals(42L, album.artworkTrackId)
        assertEquals(3, album.trackCount)
        assertEquals(2001, album.year)
    }

    @Test
    fun folderNames() {
        assertEquals("Rock", FolderNames.displayName("Music/Rock/"))
        assertEquals("Music", FolderNames.displayName("Music/"))
        assertEquals("Music", FolderNames.parentLabel("Music/Rock/"))
        assertEquals("Storage root", FolderNames.parentLabel("Music/"))
        assertEquals("Music/Rock/", FolderNames.withTrailingSlash("Music/Rock"))
        assertEquals("Music/Rock/", FolderNames.withTrailingSlash("Music/Rock/"))
    }

    @Test
    fun treeDocumentIdsAreParsed() {
        val nested = TreeUris.parse("primary:Music/Rock")
        assertEquals("primary", nested.volume)
        assertEquals("Music/Rock", nested.path)
        assertEquals("Rock", nested.displayName)
        assertEquals("Music/Rock/", nested.relativePrefix)

        val root = TreeUris.parse("primary:")
        assertEquals("", root.path)
        assertEquals("", root.relativePrefix)
        assertEquals("Internal storage", root.displayName)

        val sd = TreeUris.parse("1234-ABCD:Songs/")
        assertEquals("1234-ABCD", sd.volume)
        assertEquals("Songs", sd.path)
    }

    @Test
    fun genreWithoutNameIsUnknown() {
        assertEquals(Genre.UNKNOWN_LABEL, Genre(null, 2).displayName)
        assertEquals("Rock", Genre("Rock", 2).displayName)
    }
}
