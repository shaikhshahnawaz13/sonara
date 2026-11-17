package com.sonara.app.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SonaraTokensTest {

    @Test
    fun paletteMatchesSpecTable() {
        assertEquals(0xFF101010L, SonaraPalette.BACKGROUND)
        assertEquals(0xFF191919L, SonaraPalette.SURFACE)
        assertEquals(0xFF242424L, SonaraPalette.SURFACE_RAISED)
        assertEquals(0xFFF2F0E8L, SonaraPalette.FOREGROUND)
        assertEquals(0xFF999999L, SonaraPalette.FOREGROUND_MUTED)
        assertEquals(0xFFC6FF00L, SonaraPalette.ACCENT)
        assertEquals(0xFFF2F0E8L, SonaraPalette.BORDER)
        assertEquals(0xFF424242L, SonaraPalette.BORDER_MUTED)
        assertEquals(0xFFFF4D35L, SonaraPalette.DANGER)
    }

    @Test
    fun colorTokensUsePalette() {
        val colors = SonaraColors()
        assertEquals(Color(SonaraPalette.ACCENT), colors.accent)
        assertEquals(Color(SonaraPalette.BACKGROUND), colors.background)
        assertEquals(Color(SonaraPalette.ON_ACCENT), colors.onAccent)
    }

    @Test
    fun readableTextMeetsContrastTargets() {
        val background = SonaraPalette.BACKGROUND
        assertTrue(contrast(SonaraPalette.FOREGROUND, background) >= 7.0)
        assertTrue(contrast(SonaraPalette.ACCENT, background) >= 7.0)
        assertTrue(contrast(SonaraPalette.FOREGROUND_MUTED, background) >= 4.5)
        assertTrue(contrast(SonaraPalette.DANGER, background) >= 4.5)
        assertTrue(contrast(SonaraPalette.ON_ACCENT, SonaraPalette.ACCENT) >= 7.0)
        // Muted text must also stay readable on raised surfaces, where it is used for metadata.
        assertTrue(contrast(SonaraPalette.FOREGROUND_MUTED, SonaraPalette.SURFACE) >= 4.5)
        assertTrue(contrast(SonaraPalette.FOREGROUND_MUTED, SonaraPalette.SURFACE_RAISED) >= 4.5)
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
        assertEquals(1.dp, SonaraBorders.standard)
        assertEquals(2.dp, SonaraBorders.emphasis)
        assertTrue(SonaraSizes.minTouchTarget >= 48.dp)
    }

    @Test
    fun typeScaleStaysInsideSpecRanges() {
        val type = SonaraTypography()
        assertTrue(type.hero.fontSize.value in 40f..48f)
        assertTrue(type.screenTitle.fontSize.value in 28f..34f)
        assertTrue(type.section.fontSize.value in 18f..22f)
        assertTrue(type.trackTitle.fontSize.value in 15f..17f)
        assertEquals(14f, type.body.fontSize.value, 0f)
        assertTrue(type.metadata.fontSize.value in 11f..12f)
        assertTrue(type.microLabel.fontSize.value in 10f..11f)
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
