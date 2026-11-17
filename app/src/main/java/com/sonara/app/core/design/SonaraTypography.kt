package com.sonara.app.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Font families used by the type scale.
 *
 * Roboto is the Android system sans, so [FontFamily.SansSerif] covers the text styles.
 * Space Grotesk (display styles) is NOT bundled yet: no font file could be fetched while building M1.
 * To adopt it, add the TTFs to res/font and point [display] at them; no other file needs to change.
 */
object SonaraFonts {
    val display: FontFamily = FontFamily.SansSerif
    val text: FontFamily = FontFamily.SansSerif
}

/** Type scale from spec section 3.3. */
@Immutable
data class SonaraTypography(
    val hero: TextStyle = TextStyle(
        fontFamily = SonaraFonts.display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
    ),
    val screenTitle: TextStyle = TextStyle(
        fontFamily = SonaraFonts.display,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.25).sp,
    ),
    val section: TextStyle = TextStyle(
        fontFamily = SonaraFonts.display,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    val trackTitle: TextStyle = TextStyle(
        fontFamily = SonaraFonts.text,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = SonaraFonts.text,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    val metadata: TextStyle = TextStyle(
        fontFamily = SonaraFonts.text,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    val microLabel: TextStyle = TextStyle(
        fontFamily = SonaraFonts.text,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
    ),
)
