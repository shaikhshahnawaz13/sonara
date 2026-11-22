package com.sonara.app.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.sonara.app.data.local.database.TrackEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads every music file MediaStore knows about. The permission is checked by the caller. */
class MediaStoreScanner(
    private val resolver: ContentResolver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun scan(): List<TrackEntity> = withContext(dispatcher) {
        val rows = ArrayList<TrackEntity>()
        val cursor = resolver.query(
            COLLECTION,
            PROJECTION,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0",
            null,
            "${MediaStore.Audio.Media._ID} ASC",
        )
            // A null cursor means MediaStore did not answer, which is not the same as "no music". Failing here
            // keeps the caller from reading it as an empty library.
            ?: throw IllegalStateException("MediaStore did not answer")

        cursor.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val albumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val genre = c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
            val duration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val track = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val year = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val added = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val modified = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val size = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val path = c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val name = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (c.moveToNext()) {
                rows += TrackNormalizer.normalize(
                    RawAudioRow(
                        id = c.getLong(id),
                        title = c.getString(title),
                        artist = c.getString(artist),
                        album = c.getString(album),
                        albumArtist = c.getString(albumArtist),
                        albumId = c.getLong(albumId),
                        genre = c.getString(genre),
                        durationMs = c.getLong(duration),
                        rawTrack = c.getInt(track),
                        year = c.getInt(year),
                        dateAdded = c.getLong(added),
                        dateModified = c.getLong(modified),
                        sizeBytes = c.getLong(size),
                        mimeType = c.getString(mime),
                        relativePath = c.getString(path),
                        displayName = c.getString(name),
                    ),
                )
            }
        }
        rows
    }

    companion object {
        val COLLECTION: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        private val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )

        /** The playable content URI of a track. */
        fun trackUri(trackId: Long): Uri = ContentUris.withAppendedId(COLLECTION, trackId)

        /** MediaStore's album-art URI; opened lazily by the system when it shows notification artwork. */
        fun albumArtUri(albumId: Long): Uri =
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
    }
}
