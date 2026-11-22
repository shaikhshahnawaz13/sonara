package com.sonara.app.data.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.room.withTransaction
import com.sonara.app.data.local.database.PlaylistEntity
import com.sonara.app.data.local.database.SonaraDatabase
import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.data.local.preferences.SettingsRepository
import com.sonara.app.data.media.MediaStoreMarker
import com.sonara.app.data.media.MediaStoreScanner
import com.sonara.app.data.model.Album
import com.sonara.app.data.model.Artist
import com.sonara.app.data.model.Genre
import com.sonara.app.data.model.toAlbum
import com.sonara.app.data.model.toArtist
import com.sonara.app.data.model.toGenre
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncState {
    data object Idle : SyncState
    data object Scanning : SyncState
    data class Done(
        val total: Int,
        val added: Int,
        val updated: Int,
        val removed: Int,
        val skipped: Boolean,
        /** Songs MediaStore has that the folder exclusions keep out of the library. */
        val excluded: Int = 0,
    ) : SyncState
    data class Failed(val message: String) : SyncState
}

/**
 * The library as discovered from MediaStore and cached in Room. Only call [sync] with audio access granted.
 */
class LibraryRepository(
    private val database: SonaraDatabase,
    private val scanner: MediaStoreScanner,
    private val marker: MediaStoreMarker,
    private val settings: SettingsRepository,
    private val resolver: ContentResolver,
    private val onLibraryRescanned: () -> Unit = {},
    private val hasAudioAccess: () -> Boolean = { true },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val trackDao = database.trackDao()
    private val mutex = Mutex()
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)

    val tracks: Flow<List<TrackEntity>> = trackDao.observeAll()
    val trackCount: Flow<Int> = trackDao.observeCount()
    val syncState: StateFlow<SyncState> = _syncState

    // ---- Browsing ----
    val albumCount: Flow<Int> = trackDao.observeAlbumCount()
    val artistCount: Flow<Int> = trackDao.observeArtistCount()
    val albums: Flow<List<Album>> = trackDao.observeAlbums().map { rows -> rows.map { it.toAlbum() } }
    val artists: Flow<List<Artist>> = trackDao.observeArtists().map { rows -> rows.map { it.toArtist() } }

    /** Genres from metadata, followed by one "Unknown genre" group when some tracks have no genre. */
    val genres: Flow<List<Genre>> = combine(trackDao.observeGenres(), trackDao.observeUnknownGenreCount()) { rows, unknown ->
        rows.map { it.toGenre() } + if (unknown > 0) listOf(Genre(name = null, trackCount = unknown)) else emptyList()
    }

    fun recentlyAdded(limit: Int): Flow<List<TrackEntity>> = trackDao.observeRecentlyAdded(limit)
    fun recentlyPlayed(limit: Int): Flow<List<TrackEntity>> = trackDao.observeRecentlyPlayed(limit)

    fun album(albumId: Long): Flow<Album?> = trackDao.observeAlbum(albumId).map { it?.toAlbum() }
    fun albumTracks(albumId: Long): Flow<List<TrackEntity>> = trackDao.observeAlbumTracks(albumId)
    fun albumsOfArtist(name: String): Flow<List<Album>> =
        trackDao.observeAlbumsOfArtist(name).map { rows -> rows.map { it.toAlbum() } }
    fun artistTracks(name: String): Flow<List<TrackEntity>> = trackDao.observeArtistTracks(name)
    fun genreTracks(name: String?): Flow<List<TrackEntity>> =
        if (name == null) trackDao.observeUnknownGenreTracks() else trackDao.observeGenreTracks(name)
    fun folderTracks(path: String): Flow<List<TrackEntity>> = trackDao.observeFolderTracks(path)
    fun treeTracks(prefix: String): Flow<List<TrackEntity>> = trackDao.observeTreeTracks(prefix, prefix.length)
    val playlistCount: Flow<Int> = database.playlistDao().observeCount()
    val playlists: Flow<List<PlaylistEntity>> = database.playlistDao().observeAll()

    /**
     * Brings Room in line with MediaStore. Unless [force] is set, a scan is skipped when MediaStore reports
     * the same version/generation as after the previous scan and the folder exclusions are the same.
     *
     * Nothing is changed unless the whole scan succeeded and passed [ScanSafety]. A refused permission, a
     * failing query or an empty answer leaves the stored library, the scan marker and the last-scan time alone.
     */
    suspend fun sync(force: Boolean) = mutex.withLock {
        try {
            if (!hasAudioAccess()) {
                _syncState.value = SyncState.Failed(
                    (ScanSafety.verify(permissionGranted = false, storedCount = 0, rawScannedCount = 0) as ScanVerdict.Reject).reason,
                )
                return@withLock
            }
            val prefs = settings.libraryPrefs.first()
            val excluded = prefs.excludedFolders
            // Read the marker before scanning so a change made during the scan triggers the next one. The
            // exclusions are part of it: changing them must scan again even if MediaStore did not change.
            val currentMarker = marker.current() + FolderExclusions.stamp(excluded)
            if (!force && currentMarker == settings.settings.first().libraryScanMarker) {
                _syncState.value = SyncState.Done(
                    total = trackDao.fingerprints().size, added = 0, updated = 0, removed = 0, skipped = true,
                )
                return@withLock
            }
            _syncState.value = SyncState.Scanning
            val raw = scanner.scan()
            val stored = trackDao.fingerprints()
            when (val verdict = ScanSafety.verify(hasAudioAccess(), stored.size, raw.size)) {
                is ScanVerdict.Reject -> {
                    _syncState.value = SyncState.Failed(verdict.reason)
                    return@withLock
                }
                ScanVerdict.Accept -> Unit
            }
            val scanned = FolderExclusions.filter(raw, excluded)
            val diff = LibraryDiffer.compute(stored, scanned)
            if (!diff.isEmpty) {
                database.withTransaction {
                    diff.toUpsert.chunked(UPSERT_CHUNK).forEach { trackDao.upsertAll(it) }
                    diff.toDeleteIds.chunked(DELETE_CHUNK).forEach { trackDao.deleteByIds(it) }
                }
            }
            // Only now, with the change applied, is the scan recorded.
            settings.setLibraryScanMarker(currentMarker)
            settings.setLastScanAt(clock())
            onLibraryRescanned()
            _syncState.value = SyncState.Done(
                total = scanned.size,
                added = diff.added,
                updated = diff.updated,
                removed = diff.toDeleteIds.size,
                skipped = false,
                excluded = raw.size - scanned.size,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            _syncState.value = SyncState.Failed("Audio access was refused by the system, so the library was left as it is.")
        } catch (e: Exception) {
            _syncState.value = SyncState.Failed("The scan did not finish, so the library was left as it is. (${e.message ?: e.javaClass.simpleName})")
        }
    }

    /** Emits whenever MediaStore reports a change to audio files. */
    fun mediaStoreChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(MediaStoreScanner.COLLECTION, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    /** Ids chunked under SQLite's bound-variable limit. */
    suspend fun tracksByIds(ids: List<Long>): List<TrackEntity> {
        val byId = HashMap<Long, TrackEntity>(ids.size * 2)
        ids.distinct().chunked(LOOKUP_CHUNK).forEach { chunk ->
            trackDao.getByIds(chunk).forEach { byId[it.id] = it }
        }
        return ids.mapNotNull { byId[it] }
    }

    private companion object {
        const val UPSERT_CHUNK = 500
        const val DELETE_CHUNK = 500
        const val LOOKUP_CHUNK = 500
    }
}
