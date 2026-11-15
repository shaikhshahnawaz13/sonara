package com.sonora.music.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.sonora.music.model.Track

class LocalMusicRepository(private val context: Context) {
    fun getDeviceTracks(): List<Track> {
        val tracks = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(
            collection, projection, selection, null, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown title"
                val artist = cursor.getString(artistColumn)
                    ?.takeUnless { it == "<unknown>" } ?: "Unknown artist"
                val album = cursor.getString(albumColumn)
                    ?.takeUnless { it == "<unknown>" } ?: "Unknown album"
                val duration = cursor.getLong(durationColumn)
                val uri = ContentUris.withAppendedId(collection, id)
                tracks += Track(id, title, artist, album, duration, uri)
            }
        }
        return tracks
    }
}
