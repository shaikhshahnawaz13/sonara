package com.sonara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sonara.app.data.local.database.FavouriteEntity
import com.sonara.app.data.local.database.FolderPermissionState
import com.sonara.app.data.local.database.PlayHistoryEntity
import com.sonara.app.data.local.database.PlaylistEntryRow
import com.sonara.app.data.local.database.PlaylistRow
import com.sonara.app.data.local.database.PlaylistEntity
import com.sonara.app.data.local.database.PlaylistTrackEntity
import com.sonara.app.data.local.database.SelectedFolderEntity
import kotlinx.coroutines.flow.Flow

/** Shared by the playlist summary queries. Counts are taken against the `tracks` cache. */
internal const val PLAYLIST_SUMMARY_SELECT =
    "SELECT p.id AS id, p.name AS name, p.description AS description, p.createdAt AS createdAt, " +
        "p.updatedAt AS updatedAt, " +
        "(SELECT COUNT(*) FROM playlist_tracks e WHERE e.playlistId = p.id) AS entryCount, " +
        "(SELECT COUNT(*) FROM playlist_tracks e JOIN tracks t ON t.id = e.trackId " +
        "WHERE e.playlistId = p.id) AS trackCount, " +
        "(SELECT t.id FROM playlist_tracks e JOIN tracks t ON t.id = e.trackId " +
        "WHERE e.playlistId = p.id ORDER BY e.position ASC, e.id ASC LIMIT 1) AS artworkTrackId, " +
        "(SELECT COALESCE(SUM(t.durationMs), 0) FROM playlist_tracks e JOIN tracks t ON t.id = e.trackId " +
        "WHERE e.playlistId = p.id) AS durationMs " +
        "FROM playlists p"

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

    @Query("SELECT COUNT(*) FROM playlists")
    fun observeCount(): Flow<Int>

    // ---- M4 ----

    @Query(PLAYLIST_SUMMARY_SELECT + " ORDER BY p.name COLLATE NOCASE ASC, p.id ASC")
    fun observeSummaries(): Flow<List<PlaylistRow>>

    @Query(PLAYLIST_SUMMARY_SELECT + " WHERE p.id = :playlistId")
    fun observeSummary(playlistId: Long): Flow<PlaylistRow?>

    @Query(
        "SELECT e.id AS entryId, e.trackId AS entryTrackId, e.position AS entryPosition, t.* " +
            "FROM playlist_tracks e LEFT JOIN tracks t ON t.id = e.trackId " +
            "WHERE e.playlistId = :playlistId ORDER BY e.position ASC, e.id ASC",
    )
    fun observeEntries(playlistId: Long): Flow<List<PlaylistEntryRow>>

    /** Track ids of the entries that still exist in the library, in playlist order. */
    @Query(
        "SELECT e.trackId FROM playlist_tracks e JOIN tracks t ON t.id = e.trackId " +
            "WHERE e.playlistId = :playlistId ORDER BY e.position ASC, e.id ASC",
    )
    suspend fun playableTrackIds(playlistId: Long): List<Long>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC, id ASC")
    suspend fun entriesOf(playlistId: Long): List<PlaylistTrackEntity>

    @Query("SELECT name FROM playlists")
    suspend fun allNames(): List<String>

    @Query("SELECT name FROM playlists WHERE id = :playlistId")
    suspend fun nameOf(playlistId: Long): String?

    @Query("UPDATE playlists SET name = :name, updatedAt = :now WHERE id = :playlistId")
    suspend fun rename(playlistId: Long, name: String, now: Long)

    @Query("UPDATE playlists SET updatedAt = :now WHERE id = :playlistId")
    suspend fun touch(playlistId: Long, now: Long)

    @Query("UPDATE playlist_tracks SET position = :position WHERE id = :entryId")
    suspend fun setPosition(entryId: Long, position: Int)

    @Query("DELETE FROM playlist_tracks WHERE id = :entryId AND playlistId = :playlistId")
    suspend fun deleteEntry(playlistId: Long, entryId: Long)

    /** Removes the listed entries of one playlist. Entries of other playlists and audio files are never touched. */
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND id IN (:entryIds)")
    suspend fun deleteEntries(playlistId: Long, entryIds: List<Long>): Int

    @Query(
        "DELETE FROM playlist_tracks WHERE playlistId = :playlistId " +
            "AND trackId NOT IN (SELECT id FROM tracks)",
    )
    suspend fun deleteUnavailable(playlistId: Long): Int
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
abstract class HistoryDao {
    @Insert
    abstract suspend fun insert(entry: PlayHistoryEntity): Long

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    abstract fun observeRecent(limit: Int): Flow<List<PlayHistoryEntity>>

    @Query("DELETE FROM play_history")
    abstract suspend fun clear()

    @Query("UPDATE play_history SET playedAt = :now, playCount = playCount + 1 WHERE trackId = :trackId")
    abstract suspend fun bump(trackId: Long, now: Long): Int

    /** One row per track: a repeat play moves it to the front and counts up. */
    @Transaction
    open suspend fun recordPlay(trackId: Long, now: Long) {
        if (bump(trackId, now) == 0) insert(PlayHistoryEntity(trackId = trackId, playedAt = now))
    }
}

@Dao
interface SelectedFolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: SelectedFolderEntity): Long

    @Query("SELECT * FROM selected_folders ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SelectedFolderEntity>>

    @Query("SELECT * FROM selected_folders")
    suspend fun all(): List<SelectedFolderEntity>

    @Query("SELECT * FROM selected_folders WHERE id = :folderId")
    suspend fun byId(folderId: Long): SelectedFolderEntity?

    @Query("UPDATE selected_folders SET permissionState = :state WHERE id = :folderId")
    suspend fun updatePermissionState(folderId: Long, state: FolderPermissionState)

    @Query("DELETE FROM selected_folders WHERE id = :folderId")
    suspend fun deleteById(folderId: Long)
}
