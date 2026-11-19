package com.sonara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
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
}
