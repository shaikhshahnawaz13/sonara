package com.sonara.app.data.repository

import androidx.room.withTransaction
import com.sonara.app.data.local.database.PlaylistEntity
import com.sonara.app.data.local.database.PlaylistTrackEntity
import com.sonara.app.data.local.database.SonaraDatabase
import com.sonara.app.data.model.PlaylistEntry
import com.sonara.app.data.model.PlaylistNameError
import com.sonara.app.data.model.PlaylistRules
import com.sonara.app.data.model.PlaylistSummary
import com.sonara.app.data.model.toEntry
import com.sonara.app.data.model.toSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Outcome of creating or renaming a playlist. */
sealed interface NameResult {
    data class Saved(val playlistId: Long, val name: String) : NameResult
    data class Rejected(val error: PlaylistNameError) : NameResult
}

/** Outcome of adding tracks: how many were appended and how many were already in the playlist. */
data class AddResult(val added: Int, val alreadyThere: Int)

/**
 * Playlists in Room (tables `playlists` and `playlist_tracks`, schema unchanged since v1). Entries keep a dense
 * `position` 0..n-1; every operation that changes the set of entries or their order does so in one transaction
 * and leaves the positions dense again. Entries reference tracks by MediaStore id without a foreign key: a track
 * that disappears from the library leaves an "unavailable" entry rather than silently shrinking the playlist.
 */
class PlaylistRepository(private val database: SonaraDatabase) {
    private val dao = database.playlistDao()

    val playlists: Flow<List<PlaylistSummary>> = dao.observeSummaries().map { rows -> rows.map { it.toSummary() } }

    fun playlist(playlistId: Long): Flow<PlaylistSummary?> = dao.observeSummary(playlistId).map { it?.toSummary() }

    fun entries(playlistId: Long): Flow<List<PlaylistEntry>> =
        dao.observeEntries(playlistId).map { rows -> rows.map { it.toEntry() } }

    /** Track ids that can be played now, in order. Missing files are left out. */
    suspend fun playableTrackIds(playlistId: Long): List<Long> = dao.playableTrackIds(playlistId)

    suspend fun create(rawName: String, now: Long = System.currentTimeMillis()): NameResult =
        database.withTransaction {
            val error = PlaylistRules.validateName(rawName, dao.allNames())
            if (error != null) return@withTransaction NameResult.Rejected(error)
            val name = PlaylistRules.normalizeName(rawName)
            val id = dao.insert(PlaylistEntity(name = name, createdAt = now, updatedAt = now))
            NameResult.Saved(id, name)
        }

    suspend fun rename(playlistId: Long, rawName: String, now: Long = System.currentTimeMillis()): NameResult =
        database.withTransaction {
            val current = dao.nameOf(playlistId)
                ?: return@withTransaction NameResult.Rejected(PlaylistNameError.Empty)
            val error = PlaylistRules.validateName(rawName, dao.allNames(), current = current)
            if (error != null) return@withTransaction NameResult.Rejected(error)
            val name = PlaylistRules.normalizeName(rawName)
            dao.rename(playlistId, name, now)
            NameResult.Saved(playlistId, name)
        }

    /** Deletes the playlist and its entries. Audio files are never touched. */
    suspend fun delete(playlistId: Long) = dao.deleteById(playlistId)

    /** Appends [trackIds] in the given order, skipping tracks the playlist already has. */
    suspend fun addTracks(playlistId: Long, trackIds: List<Long>, now: Long = System.currentTimeMillis()): AddResult =
        database.withTransaction {
            val existing = dao.entriesOf(playlistId)
            val toAdd = PlaylistRules.planAppend(existing.map { it.trackId }, trackIds)
            if (toAdd.isNotEmpty()) {
                val start = existing.size
                dao.insertTracks(
                    toAdd.mapIndexed { i, trackId ->
                        PlaylistTrackEntity(playlistId = playlistId, trackId = trackId, position = start + i)
                    },
                )
                dao.touch(playlistId, now)
            }
            AddResult(added = toAdd.size, alreadyThere = trackIds.distinct().size - toAdd.size)
        }

    /** Creates a playlist and puts [trackIds] in it, atomically. */
    suspend fun createWith(rawName: String, trackIds: List<Long>, now: Long = System.currentTimeMillis()): NameResult =
        database.withTransaction {
            val result = create(rawName, now)
            if (result is NameResult.Saved) addTracks(result.playlistId, trackIds, now)
            result
        }

    suspend fun removeEntry(playlistId: Long, entryId: Long, now: Long = System.currentTimeMillis()) {
        database.withTransaction {
            dao.deleteEntry(playlistId, entryId)
            renumber(playlistId)
            dao.touch(playlistId, now)
        }
    }

    /**
     * Removes several entries in one transaction and closes the gaps. Only playlist entries go; the audio
     * files are untouched. Returns how many entries were removed (ids not in this playlist count for nothing).
     */
    suspend fun removeEntries(playlistId: Long, entryIds: Collection<Long>, now: Long = System.currentTimeMillis()): Int {
        if (entryIds.isEmpty()) return 0
        return database.withTransaction {
            var removed = 0
            // Chunked to stay well under SQLite's bound-variable limit.
            entryIds.distinct().chunked(REMOVE_CHUNK).forEach { removed += dao.deleteEntries(playlistId, it) }
            if (removed > 0) {
                renumber(playlistId)
                dao.touch(playlistId, now)
            }
            removed
        }
    }

    /** Removes entries whose audio file is no longer in the library. Returns how many were removed. */
    suspend fun removeUnavailable(playlistId: Long, now: Long = System.currentTimeMillis()): Int =
        database.withTransaction {
            val removed = dao.deleteUnavailable(playlistId)
            if (removed > 0) {
                renumber(playlistId)
                dao.touch(playlistId, now)
            }
            removed
        }

    /**
     * Moves the entry at list index [from] so that it ends at index [to]. Indices are positions in the
     * playlist as the screen shows it (every entry, available or not).
     */
    suspend fun move(playlistId: Long, from: Int, to: Int, now: Long = System.currentTimeMillis()) {
        database.withTransaction {
            val ordered = dao.entriesOf(playlistId)
            val moved = PlaylistRules.move(ordered, from, to)
            if (moved === ordered) return@withTransaction
            moved.forEachIndexed { index, entry ->
                if (entry.position != index) dao.setPosition(entry.id, index)
            }
            dao.touch(playlistId, now)
        }
    }

    /** Rewrites positions as 0..n-1 in the current order, closing any gap. */
    private suspend fun renumber(playlistId: Long) {
        dao.entriesOf(playlistId).forEachIndexed { index, entry ->
            if (entry.position != index) dao.setPosition(entry.id, index)
        }
    }

    private companion object {
        const val REMOVE_CHUNK = 500
    }
}
