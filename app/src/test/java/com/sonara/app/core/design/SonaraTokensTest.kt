package com.sonara.app.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SonaraTokensTest {

    @Test
    fun paletteMatchesTheV2Reference() {
        assertEquals(0xFFF4EFE4L, SonaraPalette.BACKGROUND)   // warm ivory
        assertEquals(0xFF123D32L, SonaraPalette.ACCENT)       // forest green
        assertEquals(0xFFFF8065L, SonaraPalette.HIGHLIGHT)    // muted coral
        assertEquals(0xFF171B18L, SonaraPalette.FOREGROUND)   // deep ink
        assertEquals(0xFFD9D1C4L, SonaraPalette.BORDER_MUTED) // subtle divider
    }

    @Test
    fun colorTokensUsePalette() {
        val colors = SonaraColors()
        assertEquals(Color(SonaraPalette.ACCENT), colors.accent)
        assertEquals(Color(SonaraPalette.BACKGROUND), colors.background)
        assertEquals(Color(SonaraPalette.ON_ACCENT), colors.onAccent)
        assertEquals(colors.borderMuted, colors.divider)
        assertEquals(colors.surfaceRaised, colors.selected)
    }

    @Test
    fun readableTextMeetsContrastTargets() {
        val surfaces = listOf(SonaraPalette.BACKGROUND, SonaraPalette.SURFACE, SonaraPalette.SURFACE_RAISED)
        surfaces.forEach { surface ->
            assertTrue("ink", contrast(SonaraPalette.FOREGROUND, surface) >= 7.0)
            assertTrue("accent text", contrast(SonaraPalette.ACCENT, surface) >= 7.0)
            assertTrue("secondary text", contrast(SonaraPalette.FOREGROUND_MUTED, surface) >= 4.5)
            assertTrue("danger", contrast(SonaraPalette.DANGER, surface) >= 4.5)
            assertTrue("warning", contrast(SonaraPalette.WARNING, surface) >= 4.5)
        }
        assertTrue("on-accent", contrast(SonaraPalette.ON_ACCENT, SonaraPalette.ACCENT) >= 7.0)
        assertTrue("on-highlight", contrast(SonaraPalette.ON_HIGHLIGHT, SonaraPalette.HIGHLIGHT) >= 4.5)
    }

    @Test
    fun graphicsMeetNonTextContrast() {
        listOf(SonaraPalette.BACKGROUND, SonaraPalette.SURFACE, SonaraPalette.SURFACE_RAISED).forEach { surface ->
            assertTrue("outline", contrast(SonaraPalette.OUTLINE, surface) >= 3.0)
            assertTrue("progress", contrast(SonaraPalette.PROGRESS, surface) >= 3.0)
        }
    }

    @Test
    fun coralIsNeverUsedAsTextOnIvory() {
        // The reference coral is too light for text; the palette keeps it a fill with ink on top. This test records why.
        assertTrue(contrast(SonaraPalette.HIGHLIGHT, SonaraPalette.BACKGROUND) < 3.0)
    }

    @Test
    fun referenceSecondaryTextWasTooLightSoItWasDeepened() {
        assertTrue(contrast(0xFF716F68L, SonaraPalette.BACKGROUND) < 4.5)
        assertTrue(contrast(SonaraPalette.FOREGROUND_MUTED, SonaraPalette.BACKGROUND) >= 4.5)
    }

    @Test
    fun colourHasAHierarchyNotAQuota() {
        // Text, accent, signal: three different roles, three different colours.
        val roles = setOf(SonaraPalette.FOREGROUND, SonaraPalette.ACCENT, SonaraPalette.HIGHLIGHT, SonaraPalette.PROGRESS)
        assertEquals(4, roles.size)
        // The dividers are quiet: far below the 3:1 an identifying outline needs.
        assertTrue(contrast(SonaraPalette.BORDER_MUTED, SonaraPalette.BACKGROUND) < 2.0)
    }

    @Test
    fun spacingFollowsFourDpScale() {
        assertEquals(
            listOf(4.dp, 8.dp, 12.dp, 16.dp, 20.dp, 24.dp, 32.dp, 40.dp),
            listOf(
                SonaraSpacing.s4, SonaraSpacing.s8, SonaraSpacing.s12, SonaraSpacing.s16,
                SonaraSpacing.s20, SonaraSpacing.s24, SonaraSpacing.s32, SonaraSpacing.s40,
            ),
        )
    }

    @Test
    fun bordersAndTouchTargetsMatchSpec() {
        assertEquals(1.dp, SonaraBorders.hairline)
        assertEquals(2.dp, SonaraBorders.rule)
        assertEquals(SonaraBorders.hairline, SonaraBorders.standard)
        assertEquals(SonaraBorders.rule, SonaraBorders.emphasis)
        assertTrue(SonaraSizes.minTouchTarget >= 48.dp)
    }

    @Test
    fun layoutTokensAreOnTheFourDpGrid() {
        listOf(
            SonaraLayout.screenMargin, SonaraLayout.gutter, SonaraLayout.itemGap, SonaraLayout.blockGap,
            SonaraLayout.sectionGap, SonaraLayout.rowMinHeight,
            SonaraArtworkSizes.row, SonaraArtworkSizes.tile, SonaraArtworkSizes.feature,
        ).forEach { assertEquals("$it", 0f, it.value % 4f, 0f) }
        assertTrue(SonaraLayout.sectionGap > SonaraLayout.blockGap && SonaraLayout.blockGap > SonaraLayout.itemGap)
        assertTrue(SonaraLayout.rowMinHeight >= SonaraSizes.minTouchTarget)
    }

    @Test
    fun typeScaleHasAClearOrderAndControlledLineHeights() {
        val type = SonaraTypography()
        val ordered = listOf(type.display, type.hero, type.screenTitle, type.section, type.trackTitle, type.body, type.metadata)
        ordered.zipWithNext().forEach { (bigger, smaller) ->
            assertTrue(bigger.fontSize.value > smaller.fontSize.value)
        }
        assertEquals(14f, type.body.fontSize.value, 0f)
        assertTrue(type.metadata.fontSize.value >= 12f)
        // Line height: about 1.0x for display, 1.1-1.2x for headings, about 1.4x for body.
        assertTrue(type.display.lineHeight.value / type.display.fontSize.value <= 1.0f)
        assertTrue(type.hero.lineHeight.value / type.hero.fontSize.value <= 1.05f)
        assertTrue(type.screenTitle.lineHeight.value / type.screenTitle.fontSize.value in 1.0f..1.2f)
        assertTrue(type.section.lineHeight.value / type.section.fontSize.value in 1.1f..1.25f)
        assertTrue(type.body.lineHeight.value / type.body.fontSize.value in 1.35f..1.5f)
    }

    @Test
    fun headingsAreCondensedAndBodyIsTheSystemSans() {
        val type = SonaraTypography()
        listOf(type.display, type.hero, type.screenTitle, type.section, type.eyebrow, type.button, type.microLabel)
            .forEach { assertEquals(SonaraFonts.display, it.fontFamily) }
        listOf(type.trackTitle, type.body, type.metadata).forEach { assertEquals(SonaraFonts.text, it.fontFamily) }
    }

    // WCAG 2.x relative luminance and contrast ratio, computed from raw ARGB constants.
    private fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
