package com.sonara.app.data.model

import com.sonara.app.data.local.database.AlbumRow
import com.sonara.app.data.local.database.ArtistRow
import com.sonara.app.data.local.database.FolderRow
import com.sonara.app.data.local.database.GenreRow

/** An album as the UI shows it. Built from [AlbumRow] by [toAlbum]. */
data class Album(
    val id: Long,
    val title: String,
    /** Album artist, or the track artist, or "Various artists" for compilations. */
    val artist: String,
    val trackCount: Int,
    val durationMs: Long,
    /** 0 when no track carries a year. */
    val year: Int,
    val artworkTrackId: Long,
)

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val artworkTrackId: Long,
)

data class Genre(
    /** Null for the group of tracks without genre metadata. */
    val name: String?,
    val trackCount: Int,
) {
    val displayName: String get() = name ?: UNKNOWN_LABEL

    companion object {
        const val UNKNOWN_LABEL = "Unknown genre"

        /** Route argument standing in for the missing-genre group (no real tag is spelled like this). */
        const val UNKNOWN_KEY = "~unknown~"
    }
}

data class Folder(
    /** MediaStore relative path with trailing slash, for example "Music/Rock/". */
    val path: String,
    val trackCount: Int,
    val durationMs: Long,
) {
    val name: String get() = FolderNames.displayName(path)
    val parentPath: String get() = FolderNames.parentLabel(path)
}

const val VARIOUS_ARTISTS = "Various artists"

fun AlbumRow.toAlbum(): Album = Album(
    id = albumId,
    title = album,
    artist = albumArtist?.takeIf { it.isNotBlank() }
        ?: if (artistCount > 1) VARIOUS_ARTISTS else artist,
    trackCount = trackCount,
    durationMs = durationMs,
    year = year,
    artworkTrackId = artworkTrackId,
)

fun ArtistRow.toArtist(): Artist = Artist(name, trackCount, albumCount, artworkTrackId)

fun GenreRow.toGenre(): Genre = Genre(name, trackCount)

fun FolderRow.toFolder(): Folder = Folder(path, trackCount, durationMs)

/** Pure helpers for folder labels and Storage Access Framework tree ids. */
object FolderNames {

    /** "Music/Rock/" -> "Rock". */
    fun displayName(path: String): String =
        path.trim('/').substringAfterLast('/').ifEmpty { path.trim('/') }.ifEmpty { "Storage" }

    /** "Music/Rock/" -> "Music". Top-level folders show "Storage root". */
    fun parentLabel(path: String): String {
        val trimmed = path.trim('/')
        return if ('/' in trimmed) trimmed.substringBeforeLast('/') else "Storage root"
    }

    /** MediaStore relative paths end with a slash; make sure ours do too. */
    fun withTrailingSlash(path: String): String = if (path.endsWith('/')) path else "$path/"
}

/** What a SAF tree document id such as "primary:Music/Rock" tells us. */
data class TreeLocation(
    val volume: String,
    /** Path inside the volume without slashes at the ends; empty for the volume root. */
    val path: String,
) {
    val displayName: String
        get() = path.substringAfterLast('/').ifEmpty { if (volume == "primary") "Internal storage" else volume }

    /** Prefix that every MediaStore relative path inside this tree starts with; empty for the volume root. */
    val relativePrefix: String
        get() = if (path.isEmpty()) "" else "$path/"
}

object TreeUris {
    fun parse(documentId: String): TreeLocation {
        val volume = documentId.substringBefore(':')
        val path = if (':' in documentId) documentId.substringAfter(':').trim('/') else ""
        return TreeLocation(volume = volume, path = path)
    }
}
