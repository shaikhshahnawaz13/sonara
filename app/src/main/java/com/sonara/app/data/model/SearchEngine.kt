package com.sonara.app.data.model

import com.sonara.app.data.local.database.PlaylistEntity
import com.sonara.app.data.local.database.TrackEntity

data class SearchResults(
    val query: String,
    val songs: List<TrackEntity> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val folders: List<Folder> = emptyList(),
    /** True when a section was cut at [SearchEngine.MAX_PER_SECTION]. */
    val truncated: Boolean = false,
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty() && folders.isEmpty()
}

/**
 * Local, case-insensitive search over what is already loaded. Every word of the query must appear
 * somewhere in an item's searchable text, so "adele hello" finds "Hello" by "Adele". Results whose
 * title starts with the query come first, then other title matches, then matches in artist or album;
 * ties are ordered alphabetically, so the order is stable.
 */
object SearchEngine {

    const val MAX_QUERY_LENGTH = 200
    const val MAX_PER_SECTION = 100

    fun normalizeQuery(raw: String): String = raw.trim().take(MAX_QUERY_LENGTH)

    fun words(query: String): List<String> =
        normalizeQuery(query).split(' ', '\t', '\n').filter { it.isNotEmpty() }

    fun search(
        query: String,
        songs: List<TrackEntity>,
        albums: List<Album>,
        artists: List<Artist>,
        playlists: List<PlaylistEntity>,
        folders: List<Folder>,
    ): SearchResults {
        val q = normalizeQuery(query)
        val terms = words(q)
        if (terms.isEmpty()) return SearchResults(query = q)

        val songHits = rank(songs, q, terms, title = { it.title }, other = { "${it.artist} ${it.album}" })
        val albumHits = rank(albums, q, terms, title = { it.title }, other = { it.artist })
        val artistHits = rank(artists, q, terms, title = { it.name }, other = { "" })
        val playlistHits = rank(playlists, q, terms, title = { it.name }, other = { it.description.orEmpty() })
        val folderHits = rank(folders, q, terms, title = { it.name }, other = { it.path })

        val truncated = listOf(songHits, albumHits, artistHits, playlistHits, folderHits)
            .any { it.size > MAX_PER_SECTION }
        return SearchResults(
            query = q,
            songs = songHits.take(MAX_PER_SECTION),
            albums = albumHits.take(MAX_PER_SECTION),
            artists = artistHits.take(MAX_PER_SECTION),
            playlists = playlistHits.take(MAX_PER_SECTION),
            folders = folderHits.take(MAX_PER_SECTION),
            truncated = truncated,
        )
    }

    private fun <T> rank(
        items: List<T>,
        query: String,
        terms: List<String>,
        title: (T) -> String,
        other: (T) -> String,
    ): List<T> {
        val scored = ArrayList<Pair<Int, T>>()
        for (item in items) {
            val t = title(item)
            val haystack = "$t ${other(item)}"
            if (terms.all { haystack.contains(it, ignoreCase = true) }) {
                val score = when {
                    t.startsWith(query, ignoreCase = true) -> 0
                    terms.all { t.contains(it, ignoreCase = true) } -> 1
                    else -> 2
                }
                scored += score to item
            }
        }
        // sortedWith is stable, so equal scores keep the incoming alphabetical order.
        return scored.sortedBy { it.first }.map { it.second }
    }
}

/** Most-recent-first list of recent searches, without duplicates (case-insensitive), capped. */
object RecentSearches {
    const val MAX = 10

    fun add(current: List<String>, query: String): List<String> {
        val q = SearchEngine.normalizeQuery(query)
        if (q.isEmpty()) return current
        return (listOf(q) + current.filterNot { it.equals(q, ignoreCase = true) }).take(MAX)
    }

    fun encode(list: List<String>): String = list.joinToString("\n")

    fun decode(raw: String?): List<String> =
        raw?.split('\n')?.map { it.trim() }?.filter { it.isNotEmpty() }?.take(MAX).orEmpty()
}
