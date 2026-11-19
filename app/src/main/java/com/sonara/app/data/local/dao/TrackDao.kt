package com.sonara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sonara.app.data.local.database.AlbumRow
import com.sonara.app.data.local.database.ArtistRow
import com.sonara.app.data.local.database.FolderRow
import com.sonara.app.data.local.database.GenreRow
import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.local.database.TrackFingerprint
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM tracks")
    fun observeCount(): Flow<Int>

    @Query("SELECT id, dateModified, sizeBytes FROM tracks")
    suspend fun fingerprints(): List<TrackFingerprint>

    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** Callers keep [ids] under SQLite's variable limit (see LibraryRepository). */
    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<TrackEntity>

    @Query("SELECT id FROM tracks")
    suspend fun allIds(): List<Long>

    // ---- Browsing (M3) ----

    @Query("SELECT COUNT(DISTINCT albumId) FROM tracks")
    fun observeAlbumCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT artist) FROM tracks")
    fun observeArtistCount(): Flow<Int>

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC, id DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int): Flow<List<TrackEntity>>

    @Query(
        "SELECT tracks.* FROM play_history INNER JOIN tracks ON tracks.id = play_history.trackId " +
            "ORDER BY play_history.playedAt DESC LIMIT :limit",
    )
    fun observeRecentlyPlayed(limit: Int): Flow<List<TrackEntity>>

    @Query(ALBUM_SELECT + " GROUP BY albumId ORDER BY album COLLATE NOCASE ASC, albumId ASC")
    fun observeAlbums(): Flow<List<AlbumRow>>

    @Query(ALBUM_SELECT + " WHERE albumId = :albumId GROUP BY albumId")
    fun observeAlbum(albumId: Long): Flow<AlbumRow?>

    @Query(ALBUM_SELECT + " WHERE artist = :artist GROUP BY albumId ORDER BY album COLLATE NOCASE ASC, albumId ASC")
    fun observeAlbumsOfArtist(artist: String): Flow<List<AlbumRow>>

    @Query(
        "SELECT artist AS name, COUNT(*) AS trackCount, COUNT(DISTINCT albumId) AS albumCount, " +
            "MIN(id) AS artworkTrackId FROM tracks GROUP BY artist ORDER BY artist COLLATE NOCASE ASC",
    )
    fun observeArtists(): Flow<List<ArtistRow>>

    @Query(
        "SELECT genre AS name, COUNT(*) AS trackCount FROM tracks WHERE genre IS NOT NULL " +
            "GROUP BY genre ORDER BY genre COLLATE NOCASE ASC",
    )
    fun observeGenres(): Flow<List<GenreRow>>

    @Query("SELECT COUNT(*) FROM tracks WHERE genre IS NULL")
    fun observeUnknownGenreCount(): Flow<Int>

    @Query(
        "SELECT relativePath AS path, COUNT(*) AS trackCount, SUM(durationMs) AS durationMs FROM tracks " +
            "WHERE relativePath IS NOT NULL GROUP BY relativePath ORDER BY relativePath COLLATE NOCASE ASC",
    )
    fun observeFolders(): Flow<List<FolderRow>>

    @Query(
        "SELECT * FROM tracks WHERE albumId = :albumId " +
            "ORDER BY discNumber ASC, trackNumber ASC, title COLLATE NOCASE ASC, id ASC",
    )
    fun observeAlbumTracks(albumId: Long): Flow<List<TrackEntity>>

    @Query(
        "SELECT * FROM tracks WHERE artist = :artist " +
            "ORDER BY album COLLATE NOCASE ASC, discNumber ASC, trackNumber ASC, title COLLATE NOCASE ASC, id ASC",
    )
    fun observeArtistTracks(artist: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE genre = :genre ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun observeGenreTracks(genre: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE genre IS NULL ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun observeUnknownGenreTracks(): Flow<List<TrackEntity>>

    /** Tracks directly inside one folder. */
    @Query("SELECT * FROM tracks WHERE relativePath = :path ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun observeFolderTracks(path: String): Flow<List<TrackEntity>>

    /** Tracks in a folder and all its subfolders. `substr` compares exactly, so no wildcard escaping is needed. */
    @Query(
        "SELECT * FROM tracks WHERE substr(relativePath, 1, :prefixLength) = :prefix " +
            "ORDER BY relativePath COLLATE NOCASE ASC, title COLLATE NOCASE ASC, id ASC",
    )
    fun observeTreeTracks(prefix: String, prefixLength: Int): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM tracks WHERE substr(relativePath, 1, :prefixLength) = :prefix")
    suspend fun countTree(prefix: String, prefixLength: Int): Int

    companion object {
        const val ALBUM_SELECT =
            "SELECT albumId, MIN(album) AS album, MIN(artist) AS artist, COUNT(DISTINCT artist) AS artistCount, " +
                "MIN(albumArtist) AS albumArtist, COUNT(*) AS trackCount, SUM(durationMs) AS durationMs, " +
                "MAX(year) AS year, MIN(id) AS artworkTrackId FROM tracks"
    }
}
