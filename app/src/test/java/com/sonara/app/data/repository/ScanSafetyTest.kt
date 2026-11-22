package com.sonara.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanSafetyTest {

    @Test
    fun noPermissionNeverAcceptsAScan() {
        assertTrue(ScanSafety.verify(permissionGranted = false, storedCount = 100, rawScannedCount = 100) is ScanVerdict.Reject)
        assertTrue(ScanSafety.verify(permissionGranted = false, storedCount = 0, rawScannedCount = 0) is ScanVerdict.Reject)
    }

    @Test
    fun anEmptyAnswerOverAFilledLibraryIsNotTrusted() {
        val verdict = ScanSafety.verify(permissionGranted = true, storedCount = 1200, rawScannedCount = 0)
        assertTrue(verdict is ScanVerdict.Reject)
        assertTrue((verdict as ScanVerdict.Reject).reason.contains("left as it is"))
    }

    @Test
    fun anEmptyAnswerOverAnEmptyLibraryIsFine() {
        assertEquals(ScanVerdict.Accept, ScanSafety.verify(true, 0, 0))
    }

    @Test
    fun aNormalScanIsAccepted() {
        assertEquals(ScanVerdict.Accept, ScanSafety.verify(true, 500, 480))
        assertEquals(ScanVerdict.Accept, ScanSafety.verify(true, 500, 1))
    }
}
