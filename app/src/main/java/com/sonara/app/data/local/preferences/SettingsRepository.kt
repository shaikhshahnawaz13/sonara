package com.sonara.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sonara.app.audio.EqualizerLogic
import com.sonara.app.audio.EqualizerSettings
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

internal object SettingsKeys {
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
        resumePlayback = this[SettingsKeys.resumePlayback] ?: defaults.resumePlayback,
        audioPermissionRequested = this[SettingsKeys.audioPermissionRequested]
            ?: defaults.audioPermissionRequested,
        notificationPermissionRequested = this[SettingsKeys.notificationPermissionRequested]
            ?: defaults.notificationPermissionRequested,
        libraryScanMarker = this[SettingsKeys.libraryScanMarker],
        recentSearches = RecentSearches.decode(this[SettingsKeys.recentSearches]),
    )
}

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
