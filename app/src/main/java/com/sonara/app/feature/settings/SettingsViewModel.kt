package com.sonara.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.app.AppContainer
import com.sonara.app.core.design.AccentChoice
import com.sonara.app.data.local.database.SonaraDatabase
import com.sonara.app.data.local.preferences.AnimationIntensity
import com.sonara.app.data.local.preferences.SongSort
import com.sonara.app.data.model.Folder
import com.sonara.app.data.storage.CacheCleaner
import com.sonara.app.data.storage.StorageReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Settings actions. Anything that must finish even if the person leaves the screen (a rescan, a change of
 * excluded folders) runs in the app scope; plain preference writes run in the screen's scope.
 */
class SettingsViewModel(private val container: AppContainer, private val appContext: Context) : ViewModel() {

    private val settings = container.settingsRepository
    private val library = container.libraryRepository

    val prefsFlow = settings.settings
    val playbackPrefs = settings.playbackPrefs
    val libraryPrefs = settings.libraryPrefs
    val syncState = library.syncState
    val audioAccess = container.audioAccess.state

    /** Folders currently in the library, for the "exclude a folder" list. */
    val libraryFolders: Flow<List<Folder>> = container.folderRepository.indexedFolders

    fun setAccent(value: AccentChoice) = viewModelScope.launch { settings.setAccent(value) }
    fun setHaptics(value: Boolean) = viewModelScope.launch { settings.setHapticsEnabled(value) }
    fun setAnimation(value: AnimationIntensity) = viewModelScope.launch { settings.setAnimationIntensity(value) }
    fun setResume(value: Boolean) = viewModelScope.launch { settings.setResumePlayback(value) }
    fun setAutoPlayNext(value: Boolean) = viewModelScope.launch { settings.setAutoPlayNext(value) }
    fun setSort(value: SongSort) = viewModelScope.launch { settings.setDefaultSort(value) }

    fun rescan() {
        container.appScope.launch { library.sync(force = true) }
    }

    /** Saves the exclusion, then scans so the library matches at once. Playlists are not touched. */
    fun exclude(path: String) {
        container.appScope.launch {
            if (settings.excludeFolder(path)) library.sync(force = true)
        }
    }

    fun include(path: String) {
        container.appScope.launch {
            settings.includeFolder(path)
            library.sync(force = true)
        }
    }

    suspend fun measureStorage(): StorageReport = withContext(Dispatchers.IO) {
        StorageReport(
            artworkMemoryBytes = container.artworkProvider.cachedBytes(),
            cacheBytes = CacheCleaner.sizeOf(appContext.cacheDir),
            libraryDatabaseBytes = databaseBytes(),
        )
    }

    /** Frees decoded artwork from memory. Nothing on disk is involved. */
    fun clearArtwork(): Long {
        val before = container.artworkProvider.cachedBytes()
        container.artworkProvider.clear()
        return before
    }

    /** Empties the app's cache folder. Never the library database, settings, playlists or any audio file. */
    suspend fun clearCacheDir(): Long = withContext(Dispatchers.IO) {
        CacheCleaner.clearContents(appContext.cacheDir).freedBytes
    }

    private fun databaseBytes(): Long {
        val main = appContext.getDatabasePath(SonaraDatabase.NAME)
        val dir = main.parentFile ?: return 0L
        return dir.listFiles { f: File -> f.name == main.name || f.name.startsWith(main.name + "-") }
            ?.sumOf { it.length() } ?: 0L
    }
}
