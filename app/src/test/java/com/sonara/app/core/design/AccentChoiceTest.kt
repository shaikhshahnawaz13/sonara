package com.sonara.app.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentChoiceTest {

    @Test
    fun forestGreenIsTheDefaultAndThePaletteAccent() {
        assertEquals(AccentChoice.Forest, AccentChoice.default)
        assertEquals(SonaraPalette.ACCENT, AccentChoice.Forest.argb)
    }

    @Test
    fun idsAreUniqueAndRoundTrip() {
        assertEquals(AccentChoice.entries.size, AccentChoice.entries.map { it.id }.toSet().size)
        AccentChoice.entries.forEach { assertEquals(it, AccentChoice.fromId(it.id)) }
    }

    @Test
    fun unknownMissingOrV1IdsFallBackToTheDefault() {
        assertEquals(AccentChoice.Forest, AccentChoice.fromId(null))
        assertEquals(AccentChoice.Forest, AccentChoice.fromId("magenta-from-the-future"))
        // Ids V1 may have saved on a phone that is upgraded.
        listOf("lime", "cyan", "orange", "pink", "ice", "amber").forEach {
            assertEquals(it, AccentChoice.Forest, AccentChoice.fromId(it))
        }
    }

    @Test
    fun everyAccentIsReadableAsTextOnTheIvorySurfaces() {
        AccentChoice.entries.forEach {
            listOf(SonaraPalette.BACKGROUND, SonaraPalette.SURFACE, SonaraPalette.SURFACE_RAISED).forEach { surface ->
                assertTrue("${it.id} on ${surface.toString(16)}", ColorContrast.ratio(it.argb, surface) >= 4.5)
            }
        }
    }

    @Test
    fun everyAccentCarriesItsOnAccentText() {
        AccentChoice.entries.forEach {
            assertTrue("${it.id} on-accent", ColorContrast.ratio(it.argb, it.onArgb) >= 4.5)
        }
    }

    @Test
    fun accentsAreDistinctFromTheTextAndSignalColours() {
        AccentChoice.entries.forEach {
            assertTrue("${it.id} vs foreground", it.argb != SonaraPalette.FOREGROUND)
            assertTrue("${it.id} vs coral", it.argb != SonaraPalette.HIGHLIGHT && it.argb != SonaraPalette.PROGRESS)
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
        val cobalt = base.withAccent(AccentChoice.Cobalt)
        assertEquals(base.copy(accent = cobalt.accent, onAccent = cobalt.onAccent), cobalt)
        assertEquals(base, base.withAccent(AccentChoice.Forest))
    }
}
