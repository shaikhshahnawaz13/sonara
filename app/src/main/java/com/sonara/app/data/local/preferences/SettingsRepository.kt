package com.sonara.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sonara.app.audio.EqualizerLogic
import com.sonara.app.audio.EqualizerSettings
import com.sonara.app.core.design.AccentChoice
import com.sonara.app.data.repository.FolderExclusions
import com.sonara.app.data.model.RecentSearches
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** How much motion the UI uses. Honoured by the motion system in M7. */
enum class AnimationIntensity { Full, Reduced, Off }

/** Persisted user settings. Only settings with a defined owner exist; more arrive with their features. */
data class SonaraSettings(
    val animationIntensity: AnimationIntensity = AnimationIntensity.Full,
    val accent: AccentChoice = AccentChoice.default,
    /** Gates every haptic the app produces. On by default: the playlist reorder pick-up always vibrated. */
    val hapticsEnabled: Boolean = true,
    val resumePlayback: Boolean = true,
    /** Internal flag: whether the audio permission dialog was ever shown (needed to detect permanent denial). */
    val audioPermissionRequested: Boolean = false,
    /** Internal flag: whether the notification permission dialog was ever shown (Android 13+). */
    val notificationPermissionRequested: Boolean = false,
    /** MediaStore version/generation after the last completed scan; null before the first scan. */
    val libraryScanMarker: String? = null,
    /** Most recent first, see RecentSearches. */
    val recentSearches: List<String> = emptyList(),
)

/** What the root of the UI needs: kept as its own small value so unrelated settings never touch the whole app. */
data class Appearance(
    val animationIntensity: AnimationIntensity = AnimationIntensity.Full,
    val accent: AccentChoice = AccentChoice.default,
    val hapticsEnabled: Boolean = true,
)

/** How Songs is ordered. [Title] is the order the app always used, so it stays the default. */
enum class SongSort(val id: String, val label: String) {
    Title("title", "Title"),
    Artist("artist", "Artist"),
    Album("album", "Album"),
    RecentlyAdded("recent", "Newest");

    companion object {
        fun fromId(id: String?): SongSort = entries.firstOrNull { it.id == id } ?: Title
    }
}

data class PlaybackPrefs(
    /** When off, playback pauses at the end of each track instead of starting the next. */
    val autoPlayNext: Boolean = true,
)

data class LibraryPrefs(
    /** Normalised folder paths (see FolderExclusions) whose songs are kept out of the library. */
    val excludedFolders: Set<String> = emptySet(),
    /** Epoch millis of the last scan that completed; null before the first one. */
    val lastScanAt: Long? = null,
    val defaultSort: SongSort = SongSort.Title,
)

internal object SettingsKeys {
    val accent = stringPreferencesKey("accent_colour")
    val haptics = booleanPreferencesKey("haptics_enabled")
    val autoPlayNext = booleanPreferencesKey("auto_play_next")
    val excludedFolders = stringSetPreferencesKey("excluded_folders")
    val lastScanAt = longPreferencesKey("last_scan_at")
    val defaultSort = stringPreferencesKey("default_song_sort")
    val equalizerEnabled = booleanPreferencesKey("equalizer_enabled")
    val equalizerPreset = stringPreferencesKey("equalizer_preset")
    val equalizerLevels = stringPreferencesKey("equalizer_band_levels")
    val animationIntensity = stringPreferencesKey("animation_intensity")
    val resumePlayback = booleanPreferencesKey("resume_playback")
    val audioPermissionRequested = booleanPreferencesKey("audio_permission_requested")
    val notificationPermissionRequested = booleanPreferencesKey("notification_permission_requested")
    val libraryScanMarker = stringPreferencesKey("library_scan_marker")
    val recentSearches = stringPreferencesKey("recent_searches")
}

/** Maps raw preferences to [SonaraSettings]; missing or unrecognised values fall back to defaults. */
internal fun Preferences.toSettings(): SonaraSettings {
    val defaults = SonaraSettings()
    return SonaraSettings(
        animationIntensity = this[SettingsKeys.animationIntensity]
            ?.let { name -> AnimationIntensity.entries.firstOrNull { it.name == name } }
            ?: defaults.animationIntensity,
        accent = AccentChoice.fromId(this[SettingsKeys.accent]),
        hapticsEnabled = this[SettingsKeys.haptics] ?: defaults.hapticsEnabled,
        resumePlayback = this[SettingsKeys.resumePlayback] ?: defaults.resumePlayback,
        audioPermissionRequested = this[SettingsKeys.audioPermissionRequested]
            ?: defaults.audioPermissionRequested,
        notificationPermissionRequested = this[SettingsKeys.notificationPermissionRequested]
            ?: defaults.notificationPermissionRequested,
        libraryScanMarker = this[SettingsKeys.libraryScanMarker],
        recentSearches = RecentSearches.decode(this[SettingsKeys.recentSearches]),
    )
}

internal fun Preferences.toPlaybackPrefs() = PlaybackPrefs(autoPlayNext = this[SettingsKeys.autoPlayNext] ?: true)

internal fun Preferences.toLibraryPrefs() = LibraryPrefs(
    excludedFolders = FolderExclusions.normalizeAll(this[SettingsKeys.excludedFolders].orEmpty()),
    lastScanAt = this[SettingsKeys.lastScanAt],
    defaultSort = SongSort.fromId(this[SettingsKeys.defaultSort]),
)

/** The equalizer's saved choice. Kept apart from [SonaraSettings] so a slider moving never touches the app-wide settings. */
internal fun Preferences.toEqualizerSettings(): EqualizerSettings = EqualizerSettings(
    enabled = this[SettingsKeys.equalizerEnabled] ?: false,
    presetName = this[SettingsKeys.equalizerPreset],
    bandLevelsMb = EqualizerLogic.decodeLevels(this[SettingsKeys.equalizerLevels]),
)

internal val Context.sonaraDataStore: DataStore<Preferences> by preferencesDataStore(name = "sonara_settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<SonaraSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toSettings() }

    val appearance: Flow<Appearance> = settings
        .map { Appearance(it.animationIntensity, it.accent, it.hapticsEnabled) }
        .distinctUntilChanged()

    val playbackPrefs: Flow<PlaybackPrefs> = safeData().map { it.toPlaybackPrefs() }.distinctUntilChanged()

    val libraryPrefs: Flow<LibraryPrefs> = safeData().map { it.toLibraryPrefs() }.distinctUntilChanged()

    private fun safeData(): Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val equalizerSettings: Flow<EqualizerSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toEqualizerSettings() }
        .distinctUntilChanged()

    suspend fun setEqualizerSettings(value: EqualizerSettings) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.equalizerEnabled] = value.enabled
            if (value.presetName != null) prefs[SettingsKeys.equalizerPreset] = value.presetName else prefs.remove(SettingsKeys.equalizerPreset)
            if (value.bandLevelsMb.isNotEmpty()) {
                prefs[SettingsKeys.equalizerLevels] = EqualizerLogic.encodeLevels(value.bandLevelsMb)
            } else {
                prefs.remove(SettingsKeys.equalizerLevels)
            }
        }
    }

    suspend fun setAnimationIntensity(value: AnimationIntensity) {
        dataStore.edit { it[SettingsKeys.animationIntensity] = value.name }
    }

    suspend fun setAccent(value: AccentChoice) {
        dataStore.edit { it[SettingsKeys.accent] = value.id }
    }

    suspend fun setHapticsEnabled(value: Boolean) {
        dataStore.edit { it[SettingsKeys.haptics] = value }
    }

    suspend fun setAutoPlayNext(value: Boolean) {
        dataStore.edit { it[SettingsKeys.autoPlayNext] = value }
    }

    suspend fun setDefaultSort(value: SongSort) {
        dataStore.edit { it[SettingsKeys.defaultSort] = value.id }
    }

    suspend fun setLastScanAt(epochMillis: Long) {
        dataStore.edit { it[SettingsKeys.lastScanAt] = epochMillis }
    }

    /** Adds a folder to the exclusions. Returns false when the path is not a usable folder path. */
    suspend fun excludeFolder(path: String): Boolean {
        val normal = FolderExclusions.normalize(path) ?: return false
        dataStore.edit { prefs ->
            prefs[SettingsKeys.excludedFolders] = FolderExclusions.normalizeAll(prefs[SettingsKeys.excludedFolders].orEmpty()) + normal
        }
        return true
    }

    suspend fun includeFolder(path: String) {
        val normal = FolderExclusions.normalize(path) ?: return
        dataStore.edit { prefs ->
            val rest = FolderExclusions.normalizeAll(prefs[SettingsKeys.excludedFolders].orEmpty()) - normal
            if (rest.isEmpty()) prefs.remove(SettingsKeys.excludedFolders) else prefs[SettingsKeys.excludedFolders] = rest
        }
    }

    suspend fun setResumePlayback(value: Boolean) {
        dataStore.edit { it[SettingsKeys.resumePlayback] = value }
    }

    suspend fun markAudioPermissionRequested() {
        dataStore.edit { it[SettingsKeys.audioPermissionRequested] = true }
    }

    suspend fun markNotificationPermissionRequested() {
        dataStore.edit { it[SettingsKeys.notificationPermissionRequested] = true }
    }

    suspend fun setLibraryScanMarker(value: String) {
        dataStore.edit { it[SettingsKeys.libraryScanMarker] = value }
    }

    suspend fun addRecentSearch(query: String) {
        dataStore.edit { prefs ->
            val current = RecentSearches.decode(prefs[SettingsKeys.recentSearches])
            prefs[SettingsKeys.recentSearches] = RecentSearches.encode(RecentSearches.add(current, query))
        }
    }

    suspend fun clearRecentSearches() {
        dataStore.edit { it.remove(SettingsKeys.recentSearches) }
    }
}
