package com.sonara.app.data.repository

import com.sonara.app.data.local.database.TrackEntity

/**
 * Which folders the person keeps out of the library. A folder is a MediaStore relative path such as
 * "Music/Podcasts/"; excluding it also excludes everything beneath it. Matching is exact and
 * case-sensitive, and a track with no known folder is never excluded.
 *
 * Exclusions only decide what the scan stores in the `tracks` cache. They never touch playlists, the
 * Favourite playlist or history, which refer to tracks by id and simply show a missing track as unavailable.
 */
object FolderExclusions {

    /** "/Music//Podcasts" -> "Music/Podcasts/". Null when nothing usable is left. */
    fun normalize(path: String): String? {
        val parts = path.replace('\\', '/').split('/').map { it.trim() }.filter { it.isNotEmpty() }
        return if (parts.isEmpty()) null else parts.joinToString(separator = "/", postfix = "/")
    }

    fun normalizeAll(paths: Set<String>): Set<String> = paths.mapNotNullTo(LinkedHashSet()) { normalize(it) }

    fun isExcluded(relativePath: String?, excluded: Set<String>): Boolean {
        if (relativePath == null || excluded.isEmpty()) return false
        val own = normalize(relativePath) ?: return false
        return excluded.any { own.startsWith(it) }
    }

    /** The tracks that remain after exclusions. Returns the same list when there are none. */
    fun filter(tracks: List<TrackEntity>, excluded: Set<String>): List<TrackEntity> =
        if (excluded.isEmpty()) tracks else tracks.filterNot { isExcluded(it.relativePath, excluded) }

    /**
     * A short stable stamp of the exclusion set. It is stored with the scan marker, so changing the exclusions
     * makes the next launch scan again even when MediaStore itself has not changed.
     */
    fun stamp(excluded: Set<String>): String {
        if (excluded.isEmpty()) return ""
        val joined = excluded.sorted().joinToString("|")
        return "#" + joined.hashCode().toUInt().toString(16)
    }

    /** The last path segment, for display ("Music/Podcasts/" -> "Podcasts"). */
    fun displayName(path: String): String = path.trimEnd('/').substringAfterLast('/')
}
