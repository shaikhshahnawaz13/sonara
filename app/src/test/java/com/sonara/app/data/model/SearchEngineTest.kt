package com.sonara.app.data.model

import com.sonara.app.data.local.database.PlaylistEntity
import com.sonara.app.data.local.database.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {

    private fun track(id: Long, title: String, artist: String = "Artist", album: String = "Album") = TrackEntity(
        id = id, title = title, artist = artist, album = album, albumArtist = null, albumId = 1, genre = null,
        durationMs = 1000, trackNumber = 0, discNumber = 0, year = 0, dateAdded = 0, dateModified = 0,
        sizeBytes = 0, mimeType = null, relativePath = null, displayName = "$title.mp3",
    )

    private fun search(
        query: String,
        songs: List<TrackEntity> = emptyList(),
        albums: List<Album> = emptyList(),
        artists: List<Artist> = emptyList(),
        playlists: List<PlaylistEntity> = emptyList(),
        folders: List<Folder> = emptyList(),
    ) = SearchEngine.search(query, songs, albums, artists, playlists, folders)

    @Test
    fun blankQueryFindsNothing() {
        val r = search("   ", songs = listOf(track(1, "Hello")))
        assertTrue(r.isEmpty)
        assertEquals("", r.query)
    }

    @Test
    fun matchingIsCaseInsensitive() {
        val r = search("hElLo", songs = listOf(track(1, "HELLO world"), track(2, "Other")))
        assertEquals(listOf(1L), r.songs.map { it.id })
    }

    @Test
    fun everyWordMustMatchAcrossTitleArtistAndAlbum() {
        val songs = listOf(
            track(1, "Hello", artist = "Adele"),
            track(2, "Hello", artist = "Lionel"),
            track(3, "Skyfall", artist = "Adele"),
        )
        assertEquals(listOf(1L), search("adele hello", songs = songs).songs.map { it.id })
        assertEquals(listOf(1L, 3L), search("adele", songs = songs).songs.map { it.id })
    }

    @Test
    fun titlePrefixRanksBeforeTitleContainsBeforeOtherFields() {
        val songs = listOf(
            track(1, "A Night Out", artist = "Zed"),
            track(2, "Night Call", artist = "Zed"),
            track(3, "Other", artist = "Night Owls"),
        )
        assertEquals(listOf(2L, 1L, 3L), search("night", songs = songs).songs.map { it.id })
    }

    @Test
    fun equalScoresKeepIncomingOrder() {
        val songs = listOf(track(5, "Alpha one"), track(2, "Alpha two"), track(9, "Alpha three"))
        assertEquals(listOf(5L, 2L, 9L), search("alpha", songs = songs).songs.map { it.id })
    }

    @Test
    fun unicodeIsMatchedCaseInsensitively() {
        val r = search("élan", songs = listOf(track(1, "ÉLAN vital")))
        assertEquals(1, r.songs.size)
    }

    @Test
    fun searchesAlbumsArtistsPlaylistsAndFolders() {
        val album = Album(1, "Night Visions", "Imagine", 10, 1000, 2012, 1)
        val artist = Artist("Night Owls", 3, 1, 1)
        val playlist = PlaylistEntity(id = 1, name = "Night Drive", description = null, createdAt = 0, updatedAt = 0)
        val folder = Folder("Music/Night Mix/", 4, 1000)
        val r = search("night", albums = listOf(album), artists = listOf(artist), playlists = listOf(playlist), folders = listOf(folder))
        assertEquals(1, r.albums.size)
        assertEquals(1, r.artists.size)
        assertEquals(1, r.playlists.size)
        assertEquals(1, r.folders.size)
        assertFalse(r.isEmpty)
    }

    @Test
    fun noMatchGivesEmptyResults() {
        assertTrue(search("zzz", songs = listOf(track(1, "Hello"))).isEmpty)
    }

    @Test
    fun overlongQueryIsCutAndStillSafe() {
        val r = search("x".repeat(5000), songs = listOf(track(1, "Hello")))
        assertEquals(SearchEngine.MAX_QUERY_LENGTH, r.query.length)
        assertTrue(r.isEmpty)
    }

    @Test
    fun sectionsAreCappedAndFlagged() {
        val songs = (1..150L).map { track(it, "Song $it") }
        val r = search("song", songs = songs)
        assertEquals(SearchEngine.MAX_PER_SECTION, r.songs.size)
        assertTrue(r.truncated)
    }

    @Test
    fun recentSearchesAreDeduplicatedCappedAndNewestFirst() {
        var list = emptyList<String>()
        list = RecentSearches.add(list, "rock")
        list = RecentSearches.add(list, "jazz")
        list = RecentSearches.add(list, "ROCK")
        assertEquals(listOf("ROCK", "jazz"), list)
        assertEquals(list, RecentSearches.add(list, "   "))
        (1..20).forEach { list = RecentSearches.add(list, "q$it") }
        assertEquals(RecentSearches.MAX, list.size)
        assertEquals("q20", list.first())
    }

    @Test
    fun recentSearchesRoundTripThroughEncoding() {
        val list = listOf("one", "two words")
        assertEquals(list, RecentSearches.decode(RecentSearches.encode(list)))
        assertEquals(emptyList<String>(), RecentSearches.decode(null))
        assertEquals(emptyList<String>(), RecentSearches.decode(""))
    }
}
