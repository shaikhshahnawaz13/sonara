package com.sonara.app.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw ARGB values for the SONARA palette (spec section 3.2).
 * Kept as plain constants so they can be unit-tested without Compose types.
 */
internal object SonaraPalette {
    const val BACKGROUND = 0xFF101010L
    const val SURFACE = 0xFF191919L
    const val SURFACE_RAISED = 0xFF242424L
    const val FOREGROUND = 0xFFF2F0E8L
    const val FOREGROUND_MUTED = 0xFF999999L
    const val ACCENT = 0xFFC6FF00L
    const val BORDER = 0xFFF2F0E8L
    const val BORDER_MUTED = 0xFF424242L
    const val DANGER = 0xFFFF4D35L

    /** Not in the spec table: text/icon colour on top of [ACCENT]. Equals the background for maximum contrast. */
    const val ON_ACCENT = BACKGROUND
}

/** Semantic colour tokens. UI code reads these via [SonaraTheme.colors]; never hardcode hex values. */
@Immutable
data class SonaraColors(
    val background: Color = Color(SonaraPalette.BACKGROUND),
    val surface: Color = Color(SonaraPalette.SURFACE),
    val surfaceRaised: Color = Color(SonaraPalette.SURFACE_RAISED),
    val foreground: Color = Color(SonaraPalette.FOREGROUND),
    val foregroundMuted: Color = Color(SonaraPalette.FOREGROUND_MUTED),
    val accent: Color = Color(SonaraPalette.ACCENT),
    val onAccent: Color = Color(SonaraPalette.ON_ACCENT),
    val border: Color = Color(SonaraPalette.BORDER),
    val borderMuted: Color = Color(SonaraPalette.BORDER_MUTED),
    val danger: Color = Color(SonaraPalette.DANGER),
)

/** The palette with the chosen accent. Everything that is not the accent is fixed. */
fun SonaraColors.withAccent(choice: AccentChoice): SonaraColors =
    if (choice == AccentChoice.default) this else copy(accent = Color(choice.argb), onAccent = Color(choice.onArgb))
