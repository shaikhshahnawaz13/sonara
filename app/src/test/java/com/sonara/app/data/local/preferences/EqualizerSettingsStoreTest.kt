package com.sonara.app.data.local.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.sonara.app.audio.EqualizerSettings
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The equalizer's choice is written to a real DataStore file and read back through the repository's own decoding. */
class EqualizerSettingsStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun repository(): SettingsRepository {
        val store = PreferenceDataStoreFactory.create(produceFile = { File(tempFolder.root, "eq.preferences_pb") })
        return SettingsRepository(store)
    }

    @Test
    fun emptyStoreIsOffAndFlat() = runBlocking {
        assertEquals(EqualizerSettings(), repository().equalizerSettings.first())
    }

    @Test
    fun enabledPresetAndLevelsAreReadBack() = runBlocking {
        val repo = repository()
        val saved = EqualizerSettings(enabled = true, presetName = "Rock", bandLevelsMb = listOf(500, 300, -200, 200, 500))
        repo.setEqualizerSettings(saved)
        assertEquals(saved, repo.equalizerSettings.first())
    }

    @Test
    fun customCurveHasNoPresetName() = runBlocking {
        val repo = repository()
        repo.setEqualizerSettings(EqualizerSettings(enabled = true, presetName = "Rock", bandLevelsMb = listOf(1, 2, 3)))
        val custom = EqualizerSettings(enabled = true, presetName = null, bandLevelsMb = listOf(100, 0, -50))
        repo.setEqualizerSettings(custom)
        assertEquals(custom, repo.equalizerSettings.first())
    }

    @Test
    fun turningItOffKeepsTheCurve() = runBlocking {
        val repo = repository()
        val on = EqualizerSettings(enabled = true, presetName = null, bandLevelsMb = listOf(100, 200, 300))
        repo.setEqualizerSettings(on)
        repo.setEqualizerSettings(on.copy(enabled = false))
        assertEquals(on.copy(enabled = false), repo.equalizerSettings.first())
    }

    @Test
    fun equalizerChoiceDoesNotChangeTheAppSettings() = runBlocking {
        val repo = repository()
        val before = repo.settings.first()
        repo.setEqualizerSettings(EqualizerSettings(enabled = true, bandLevelsMb = listOf(1, 2)))
        assertEquals(before, repo.settings.first())
    }
}
