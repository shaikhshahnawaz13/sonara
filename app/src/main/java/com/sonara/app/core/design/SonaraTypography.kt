package com.sonara.app.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sonara.app.R

/**
 * Font families used by the type scale.
 *
 *  - [display] is Barlow Condensed (SIL Open Font Licence 1.1), bundled in res/font in three weights: Medium,
 *    SemiBold and Bold. A request for any other weight resolves to the nearest of these. It is used for headings,
 *    kickers, buttons and labels: the narrow, tall letterforms are the editorial voice of SONARA.
 *  - [text] is the Android system sans (Roboto). Nothing is bundled for body text: it is the most legible choice
 *    on every device and costs nothing.
 *
 * The licence text is kept in docs/licences/BarlowCondensed-OFL.txt and the font is credited in Settings > About >
 * Open-source licences.
 */
object SonaraFonts {
    val display: FontFamily = FontFamily(
        Font(R.font.barlow_condensed_medium, FontWeight.Medium),
        Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    )
    val text: FontFamily = FontFamily.SansSerif
}

/**
 * V2 type scale. Headings are condensed and tight, body is open and quiet, and the same few weights repeat
 * everywhere (display: SemiBold/Bold, text: Normal/Medium). Condensed styles are meant for upper-case kickers
 * and short headings; long reading text stays on [SonaraFonts.text].
 *
 * Line heights are set explicitly: about 1.0x for large display, 1.1-1.2x for headings, 1.4x for body.
 */
@Immutable
data class SonaraTypography(
    /** Very large editorial numerals and opening titles. Use sparingly: once per screen at most. */
    val display: TextStyle = TextStyle(
        fontFamily = SonaraFonts.display,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.5).sp,
    ),
    val hero: TextStyle = TextStyle(
        fontFamily = SonaraFonts.display,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.25).sp,
    ),
    val screenTitle: TextStyle = TextStyle(
        fontFamily = SonaraFonts.display,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 36.sp,
    ),
    val section: TextStyle = TextStyle(
        fontFamily = SonaraFonts.display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.2.sp,
    ),
    /** Small upper-case kicker above a heading ("RECENTLY ADDED"). Callers pass the text already upper-cased. */
    val eyebrow: TextStyle = TextStyle(
        fontFamily = SonaraFonts.display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.6.sp,
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
        letterSpacing = 0.1.sp,
    ),
    val metadata: TextStyle = TextStyle(
        fontFamily = SonaraFonts.text,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    val button: TextStyle = TextStyle(
        fontFamily = SonaraFonts.display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 1.0.sp,
    ),
    val microLabel: TextStyle = TextStyle(
        fontFamily = SonaraFonts.display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.0.sp,
    ),
)
