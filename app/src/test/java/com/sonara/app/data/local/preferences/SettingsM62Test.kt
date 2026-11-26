package com.sonara.app.data.local.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sonara.app.core.design.AccentChoice
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsM62Test {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun store() = PreferenceDataStoreFactory.create(produceFile = { File(tempFolder.root, "m62.preferences_pb") })

    @Test
    fun defaultsPreserveTheExistingExperience() {
        val repo = SettingsRepository(store())
        runBlocking {
            val s = repo.settings.first()
            assertEquals(AccentChoice.Forest, s.accent)
            assertTrue(s.hapticsEnabled)
            assertEquals(AnimationIntensity.Full, s.animationIntensity)
            assertTrue(s.resumePlayback)
            assertTrue(repo.playbackPrefs.first().autoPlayNext)
            val lib = repo.libraryPrefs.first()
            assertTrue(lib.excludedFolders.isEmpty())
            assertNull(lib.lastScanAt)
            assertEquals(SongSort.Title, lib.defaultSort)
        }
    }

    @Test
    fun accentHapticsAndAutoPlayPersist() {
        val repo = SettingsRepository(store())
        runBlocking {
            repo.setAccent(AccentChoice.Cobalt)
            repo.setHapticsEnabled(false)
            repo.setAutoPlayNext(false)
            assertEquals(AccentChoice.Cobalt, repo.settings.first().accent)
            assertFalse(repo.settings.first().hapticsEnabled)
            assertFalse(repo.playbackPrefs.first().autoPlayNext)
            assertEquals(Appearance(AnimationIntensity.Full, AccentChoice.Cobalt, false), repo.appearance.first())
        }
    }

    @Test
    fun existingSettingsAreNotDisturbedByTheNewOnes() {
        val repo = SettingsRepository(store())
        runBlocking {
            repo.setAnimationIntensity(AnimationIntensity.Reduced)
            repo.setResumePlayback(false)
            repo.setLibraryScanMarker("7:42")
            repo.addRecentSearch("queen")
            repo.setAccent(AccentChoice.Plum)
            repo.excludeFolder("Music/Podcasts")
            repo.setDefaultSort(SongSort.Album)
            val s = repo.settings.first()
            assertEquals(AnimationIntensity.Reduced, s.animationIntensity)
            assertFalse(s.resumePlayback)
            assertEquals("7:42", s.libraryScanMarker)
            assertEquals(listOf("queen"), s.recentSearches)
        }
    }

    @Test
    fun unknownSavedValuesFallBackSafely() {
        val ds = store()
        runBlocking {
            ds.edit {
                it[stringPreferencesKey("accent_colour")] = "chartreuse-9000"
                it[stringPreferencesKey("default_song_sort")] = "by-mood"
            }
            val repo = SettingsRepository(ds)
            assertEquals(AccentChoice.Forest, repo.settings.first().accent)
            assertEquals(SongSort.Title, repo.libraryPrefs.first().defaultSort)
        }
        assertEquals(AccentChoice.Forest, emptyPreferences().toSettings().accent)
    }

    @Test
    fun excludedFoldersAddRemoveAndNormalise() {
        val repo = SettingsRepository(store())
        runBlocking {
            assertTrue(repo.excludeFolder("Music/Podcasts"))
            assertTrue(repo.excludeFolder("/Music/Podcasts/"))
            assertTrue(repo.excludeFolder("Audiobooks/"))
            assertFalse(repo.excludeFolder("   "))
            assertEquals(setOf("Music/Podcasts/", "Audiobooks/"), repo.libraryPrefs.first().excludedFolders)
            repo.includeFolder("music/podcasts") // case differs: a different folder, nothing removed
            assertEquals(2, repo.libraryPrefs.first().excludedFolders.size)
            repo.includeFolder("Music/Podcasts")
            assertEquals(setOf("Audiobooks/"), repo.libraryPrefs.first().excludedFolders)
            repo.includeFolder("Audiobooks/")
            assertTrue(repo.libraryPrefs.first().excludedFolders.isEmpty())
        }
    }

    @Test
    fun lastScanTimeIsStored() {
        val repo = SettingsRepository(store())
        runBlocking {
            repo.setLastScanAt(1_700_000_000_000L)
            assertEquals(1_700_000_000_000L, repo.libraryPrefs.first().lastScanAt)
        }
    }

    @Test
    fun sortIdsRoundTrip() {
        SongSort.entries.forEach { assertEquals(it, SongSort.fromId(it.id)) }
        assertEquals(SongSort.Title, SongSort.fromId(null))
    }
}
