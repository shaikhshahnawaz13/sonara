package com.sonara.app.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentChoiceTest {

    @Test
    fun limeIsTheDefaultAndTheBrandColour() {
        assertEquals(AccentChoice.Lime, AccentChoice.default)
        assertEquals(SonaraPalette.ACCENT, AccentChoice.Lime.argb)
    }

    @Test
    fun idsAreUniqueAndRoundTrip() {
        assertEquals(AccentChoice.entries.size, AccentChoice.entries.map { it.id }.toSet().size)
        AccentChoice.entries.forEach { assertEquals(it, AccentChoice.fromId(it.id)) }
    }

    @Test
    fun unknownOrMissingIdFallsBackToLime() {
        assertEquals(AccentChoice.Lime, AccentChoice.fromId(null))
        assertEquals(AccentChoice.Lime, AccentChoice.fromId("magenta-from-the-future"))
    }

    @Test
    fun everyAccentIsReadableAsTextOnTheDarkSurfaces() {
        AccentChoice.entries.forEach {
            assertTrue("${it.id} on background", ColorContrast.ratio(it.argb, SonaraPalette.BACKGROUND) >= 4.5)
            assertTrue("${it.id} on raised", ColorContrast.ratio(it.argb, SonaraPalette.SURFACE_RAISED) >= 4.5)
        }
    }

    @Test
    fun everyAccentCarriesItsOnAccentText() {
        AccentChoice.entries.forEach {
            assertTrue("${it.id} on-accent", ColorContrast.ratio(it.argb, it.onArgb) >= 4.5)
        }
    }

    @Test
    fun accentsAreDistinctFromTheForegroundText() {
        AccentChoice.entries.forEach {
            assertTrue("${it.id} vs foreground", it.argb != SonaraPalette.FOREGROUND)
        }
    }

    @Test
    fun contrastRatioKnownValues() {
        assertEquals(21.0, ColorContrast.ratio(0xFFFFFFFFL, 0xFF000000L), 0.01)
        assertEquals(1.0, ColorContrast.ratio(0xFF123456L, 0xFF123456L), 0.0001)
    }

    @Test
    fun onlyTheAccentPairChangesInThePalette() {
        val base = SonaraColors()
        val cyan = base.withAccent(AccentChoice.Cyan)
        assertEquals(base.copy(accent = cyan.accent, onAccent = cyan.onAccent), cyan)
        assertEquals(base, base.withAccent(AccentChoice.Lime))
    }
}
