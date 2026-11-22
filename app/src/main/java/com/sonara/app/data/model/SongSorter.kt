package com.sonara.app.data.model

import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.local.preferences.SongSort

/**
 * Orders the Songs list. [SongSort.Title] returns the list untouched: the database already delivers it in
 * that order, and it is what the app has always shown.
 */
object SongSorter {

    private val byTitle = compareBy<TrackEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.title }

    fun sort(tracks: List<TrackEntity>, sort: SongSort): List<TrackEntity> = when (sort) {
        SongSort.Title -> tracks
        SongSort.Artist -> tracks.sortedWith(
            compareBy<TrackEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.album }
                .thenBy { it.discNumber }
                .thenBy { it.trackNumber }
                .then(byTitle)
                .thenBy { it.id },
        )
        SongSort.Album -> tracks.sortedWith(
            compareBy<TrackEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.album }
                .thenBy { it.discNumber }
                .thenBy { it.trackNumber }
                .then(byTitle)
                .thenBy { it.id },
        )
        SongSort.RecentlyAdded -> tracks.sortedWith(
            compareByDescending<TrackEntity> { it.dateAdded }.thenByDescending { it.id },
        )
    }
}
