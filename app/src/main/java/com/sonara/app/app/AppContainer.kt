package com.sonara.app.app

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.sonara.app.core.permissions.AudioAccessModel
import com.sonara.app.core.permissions.AudioAccessState
import com.sonara.app.core.permissions.AudioPermissionChecker
import com.sonara.app.data.local.database.SonaraDatabase
import com.sonara.app.data.local.preferences.SettingsRepository
import com.sonara.app.data.local.preferences.sonaraDataStore
import com.sonara.app.data.media.ArtworkProvider
import com.sonara.app.data.media.MediaStoreMarker
import com.sonara.app.data.media.MediaStoreScanner
import com.sonara.app.data.repository.FolderRepository
import com.sonara.app.data.repository.LibraryRepository
import com.sonara.app.data.repository.PlaylistRepository
import com.sonara.app.feature.playlists.PlaylistPrefetch
import com.sonara.app.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

/**
 * Manual dependency container, created once by [SonaraApplication].
 * Everything is lazy so nothing opens the database or reads preferences during process start.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Process-wide scope for work that must outlive any screen or Activity. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: SonaraDatabase by lazy { SonaraDatabase.create(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext.sonaraDataStore) }
    val audioPermissionChecker: AudioPermissionChecker by lazy { AudioPermissionChecker(appContext) }
    val audioAccess: AudioAccessModel by lazy {
        AudioAccessModel(audioPermissionChecker, settingsRepository, appScope)
    }

    val artworkProvider: ArtworkProvider by lazy { ArtworkProvider(appContext.contentResolver) }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(
            database = database,
            scanner = MediaStoreScanner(appContext.contentResolver),
            marker = MediaStoreMarker(appContext),
            settings = settingsRepository,
            resolver = appContext.contentResolver,
            onLibraryRescanned = { artworkProvider.clearMisses() },
        )
    }

    val folderRepository: FolderRepository by lazy { FolderRepository(database, appContext.contentResolver) }

    val playlistRepository: PlaylistRepository by lazy { PlaylistRepository(database) }

    /** Fetches a playlist's data and first-screen artwork between a tap and the screen sliding in. */
    val playlistPrefetch: PlaylistPrefetch by lazy { PlaylistPrefetch(playlistRepository, artworkProvider, appScope) }

    val playbackController: PlaybackController by lazy { PlaybackController(appContext, appScope) }

    /**
     * Starts the work that follows audio access: once access is granted, scan MediaStore (skipped when nothing
     * changed since the last scan) and keep the library current while the process lives.
     */
    @OptIn(FlowPreview::class)
    fun start() {
        audioAccess.refresh(shouldShowRationale = null)
        // The Favourite playlist must exist before anyone looks for it, including in a database from before it
        // was system-managed. Off the main thread; a failure is retried by the heart itself on its first tap.
        appScope.launch(Dispatchers.IO) { runCatching { playlistRepository.ensureFavourites() } }
        appScope.launch {
            audioAccess.state
                .map { it == AudioAccessState.Granted }
                .distinctUntilChanged()
                .collectLatest { granted ->
                    if (!granted) return@collectLatest
                    libraryRepository.sync(force = false)
                    libraryRepository.mediaStoreChanges()
                        .debounce(MEDIASTORE_DEBOUNCE_MS)
                        .collect { libraryRepository.sync(force = false) }
                }
        }
    }

    private companion object {
        const val MEDIASTORE_DEBOUNCE_MS = 1_500L
    }
}

/** Gives screens access to the container until ViewModels arrive with the library screens (M3). */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
