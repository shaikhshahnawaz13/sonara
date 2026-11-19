package com.sonara.app.data.local.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room holds SONARA-owned data (spec section 8). The rows below reference tracks by their MediaStore id.
 * The `tracks` cache (TrackEntity) is a rebuildable copy of MediaStore added in M2; albums and artists are
 * not stored, they are derived from tracks when the Library screens need them (M3).
 */

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** One playlist entry. [id] is a surrogate key so a track can be reordered or repeated without key clashes. */
@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["playlistId", "position"]),
        Index(value = ["trackId"]),
    ],
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
)

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val trackId: Long,
    val createdAt: Long,
)

@Entity(
    tableName = "play_history",
    indices = [Index(value = ["trackId"]), Index(value = ["playedAt"])],
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val playedAt: Long,
    val playCount: Int = 1,
)

enum class FolderPermissionState { Granted, Revoked, Unavailable }

@Entity(
    tableName = "selected_folders",
    indices = [Index(value = ["treeUri"], unique = true)],
)
data class SelectedFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treeUri: String,
    val displayName: String,
    val permissionState: FolderPermissionState,
)
