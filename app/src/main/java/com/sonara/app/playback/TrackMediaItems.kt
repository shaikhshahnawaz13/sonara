package com.sonara.app.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.media.MediaStoreScanner

/**
 * A fully described, playable item. Built only inside the playback service.
 * The media id is the MediaStore id as text, which is how every other part of SONARA refers to a track.
 */
fun TrackEntity.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(MediaStoreScanner.trackUri(id))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setAlbumArtist(albumArtist)
                .setTrackNumber(trackNumber.takeIf { it > 0 })
                .setArtworkUri(MediaStoreScanner.albumArtUri(albumId))
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build(),
        )
        .build()

/**
 * A lightweight request item carrying only the id. Controllers send these so that large queues stay small
 * across the process boundary; the service resolves them against the library.
 */
fun trackRequestItem(trackId: Long): MediaItem =
    MediaItem.Builder().setMediaId(trackId.toString()).build()
