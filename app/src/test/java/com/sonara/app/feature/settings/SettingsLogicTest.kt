package com.sonara.app.feature.settings

import com.sonara.app.data.repository.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLogicTest {

    private val now = 1_000_000_000_000L

    @Test
    fun lastScanWording() {
        assertEquals("Never scanned", SettingsLogic.lastScanText(null, now))
        assertEquals("Just now", SettingsLogic.lastScanText(now - 5_000, now))
        assertEquals("Just now", SettingsLogic.lastScanText(now + 60_000, now)) // clock moved back
        assertEquals("12 min ago", SettingsLogic.lastScanText(now - 12 * 60_000L, now))
        assertEquals("3 h ago", SettingsLogic.lastScanText(now - 3 * 3_600_000L, now))
        assertEquals("5 d ago", SettingsLogic.lastScanText(now - 5 * 86_400_000L, now))
    }

    @Test
    fun scanResultWording() {
        assertNull(SettingsLogic.scanResultText(SyncState.Idle))
        assertEquals("Scanning your music…", SettingsLogic.scanResultText(SyncState.Scanning))
        assertEquals("Boom", SettingsLogic.scanResultText(SyncState.Failed("Boom")))
        assertEquals("Library is up to date (10 songs)", SettingsLogic.scanResultText(SyncState.Done(10, 0, 0, 0, skipped = true)))
        assertEquals("Library is up to date · 10 songs", SettingsLogic.scanResultText(SyncState.Done(10, 0, 0, 0, skipped = false)))
        assertEquals("3 added · 1 removed · 12 songs", SettingsLogic.scanResultText(SyncState.Done(12, 3, 0, 1, skipped = false)))
        val withExcluded = SettingsLogic.scanResultText(SyncState.Done(8, 0, 0, 4, skipped = false, excluded = 4))!!
        assertTrue(withExcluded.contains("4 songs kept out by excluded folders"))
    }

    @Test
    fun singularSong() {
        assertEquals("1 song", SettingsLogic.songs(1))
        assertEquals("0 songs", SettingsLogic.songs(0))
    }

    @Test
    fun versionText() {
        assertEquals("1.0.0-m6.2 (11)", SettingsLogic.versionText("1.0.0-m6.2", 11, debuggable = false))
        assertEquals("1.0.0-m6.2 (11) · debug build", SettingsLogic.versionText("1.0.0-m6.2", 11, debuggable = true))
        assertEquals("unknown (3)", SettingsLogic.versionText(null, 3, false))
    }

    @Test
    fun statusAndCountWording() {
        assertEquals("Allowed", SettingsLogic.notificationStatus(true))
        assertTrue(SettingsLogic.notificationStatus(false).startsWith("Off"))
        assertEquals("None", SettingsLogic.excludedCountText(0))
        assertEquals("1 folder", SettingsLogic.excludedCountText(1))
        assertEquals("4 folders", SettingsLogic.excludedCountText(4))
    }

    @Test
    fun clearedWording() {
        assertEquals("artwork cache was already empty", SettingsLogic.clearedText(0, "artwork cache"))
        assertEquals("Cleared app cache (2.0 KB)", SettingsLogic.clearedText(2048, "app cache"))
    }

    @Test
    fun noticesHaveNoVersionNumbersAndAllNameALicence() {
        OpenSourceNotices.all.forEach {
            assertTrue(it.name, it.licence.isNotBlank() && it.author.isNotBlank())
            assertTrue(it.name, !Regex("\\d+\\.\\d+").containsMatchIn(it.name))
        }
    }
}
