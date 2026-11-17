package com.sonara.app.data.local.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun newRepository(): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempFolder.root, "test.preferences_pb") },
        )
        return SettingsRepository(dataStore)
    }

    @Test
    fun emptyStoreYieldsDefaults() {
        runBlocking {
            val settings = newRepository().settings.first()
            assertEquals(SonaraSettings(), settings)
            assertEquals(AnimationIntensity.Full, settings.animationIntensity)
            assertTrue(settings.resumePlayback)
            assertFalse(settings.audioPermissionRequested)
        }
    }

    @Test
    fun writtenValuesAreReadBack() {
        runBlocking {
            val repository = newRepository()
            repository.setAnimationIntensity(AnimationIntensity.Reduced)
            repository.setResumePlayback(false)
            repository.markAudioPermissionRequested()

            val settings = repository.settings.first()
            assertEquals(AnimationIntensity.Reduced, settings.animationIntensity)
            assertFalse(settings.resumePlayback)
            assertTrue(settings.audioPermissionRequested)
        }
    }

    @Test
    fun unknownStoredEnumFallsBackToDefault() {
        val prefs = preferencesOf(stringPreferencesKey("animation_intensity") to "Ludicrous")
        assertEquals(AnimationIntensity.Full, prefs.toSettings().animationIntensity)
    }

    @Test
    fun knownStoredEnumIsParsed() {
        val prefs = preferencesOf(stringPreferencesKey("animation_intensity") to "Off")
        assertEquals(AnimationIntensity.Off, prefs.toSettings().animationIntensity)
    }
}
