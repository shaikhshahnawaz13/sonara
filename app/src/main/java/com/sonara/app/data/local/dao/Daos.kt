package com.sonara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonara.app.data.local.database.FavouriteEntity
import com.sonara.app.data.local.database.FolderPermissionState
import com.sonara.app.data.local.database.PlayHistoryEntity
import com.sonara.app.data.local.database.PlaylistEntity
import com.sonara.app.data.local.database.PlaylistTrackEntity
import com.sonara.app.data.local.database.SelectedFolderEntity
import kotlinx.coroutines.flow.Flow

/** Deleting a playlist cascades to its entries only; audio files are never touched. */
@Dao
interface PlaylistDao {
    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deleteById(playlistId: Long)

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Insert
    suspend fun insertTracks(tracks: List<PlaylistTrackEntity>)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>
}

@Dao
interface FavouriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favourite: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: Long)

    @Query("SELECT trackId FROM favourites ORDER BY createdAt DESC")
    fun observeTrackIds(): Flow<List<Long>>
}

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: PlayHistoryEntity): Long

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlayHistoryEntity>>

    @Query("DELETE FROM play_history")
    suspend fun clear()
}

@Dao
interface SelectedFolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: SelectedFolderEntity): Long

    @Query("SELECT * FROM selected_folders ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SelectedFolderEntity>>

    @Query("UPDATE selected_folders SET permissionState = :state WHERE id = :folderId")
    suspend fun updatePermissionState(folderId: Long, state: FolderPermissionState)

    @Query("DELETE FROM selected_folders WHERE id = :folderId")
    suspend fun deleteById(folderId: Long)
}
