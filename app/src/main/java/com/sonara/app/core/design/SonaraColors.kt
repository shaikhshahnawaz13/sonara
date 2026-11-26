package com.sonara.app.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw ARGB values for the SONARA V2 palette: warm ivory, forest green, muted coral, deep ink.
 * Kept as plain constants so they can be unit-tested without Compose types.
 *
 * Colour has a hierarchy, not a quota:
 *  - ink on ivory carries almost everything (text, icons, rules);
 *  - forest green ([ACCENT]) is the one interactive colour: the primary button, text actions, the active destination;
 *  - coral ([HIGHLIGHT], [PROGRESS]) is a rare signal: playback progress, the track that is playing, a chosen chip.
 *    It is too light to be text on ivory, so it never is: [HIGHLIGHT] is a fill with ink on it, and [PROGRESS]
 *    is a darker coral for thin graphics.
 */
internal object SonaraPalette {
    // Surfaces
    const val BACKGROUND = 0xFFF4EFE4L          // warm ivory
    const val SURFACE = 0xFFEEE8DBL             // slightly deeper ivory: panels, placeholders
    const val SURFACE_RAISED = 0xFFE8E1D2L      // pressed and selected backgrounds

    // Text and rules
    const val FOREGROUND = 0xFF171B18L          // deep ink
    /** Secondary text. The reference value #716F68 is 4.39:1 on ivory, under the 4.5:1 target, so this is a touch deeper. */
    const val FOREGROUND_MUTED = 0xFF65635BL
    const val BORDER = FOREGROUND               // strong rule
    const val BORDER_MUTED = 0xFFD9D1C4L        // subtle divider (hairlines; decorative, not an identifying outline)
    const val OUTLINE = 0xFF85806FL             // outline that identifies a control (3:1 against every surface)

    // Interactive colour (primary action, text actions, selected destination)
    const val ACCENT = 0xFF123D32L              // forest green
    const val ON_ACCENT = BACKGROUND

    // Signals
    const val HIGHLIGHT = 0xFFFF8065L           // muted coral: a fill, always with ON_HIGHLIGHT on it
    const val ON_HIGHLIGHT = FOREGROUND
    const val PROGRESS = 0xFFC8452BL            // deeper coral for thin graphics (3:1 against every surface)

    // States
    const val DANGER = 0xFFB3261EL
    const val WARNING = 0xFF8A5700L
    const val DISABLED_CONTENT = 0xFFA8A396L    // deliberately quiet; disabled controls are exempt from contrast targets
    const val DISABLED_CONTAINER = 0xFFE6E0D3L
}

/**
 * Semantic colour tokens. UI code reads these via [SonaraTheme.colors]; never hardcode hex values.
 *
 * The first group keeps the names the V1 screens already use, so they pick up V2 without edits.
 * The second group is new in V2.
 */
@Immutable
data class SonaraColors(
    val background: Color = Color(SonaraPalette.BACKGROUND),
    val surface: Color = Color(SonaraPalette.SURFACE),
    val surfaceRaised: Color = Color(SonaraPalette.SURFACE_RAISED),
    val foreground: Color = Color(SonaraPalette.FOREGROUND),
    val foregroundMuted: Color = Color(SonaraPalette.FOREGROUND_MUTED),
    /** Primary action and the colour the person can change in Settings. */
    val accent: Color = Color(SonaraPalette.ACCENT),
    val onAccent: Color = Color(SonaraPalette.ON_ACCENT),
    val border: Color = Color(SonaraPalette.BORDER),
    val borderMuted: Color = Color(SonaraPalette.BORDER_MUTED),
    val danger: Color = Color(SonaraPalette.DANGER),

    // New in V2
    val outline: Color = Color(SonaraPalette.OUTLINE),
    val highlight: Color = Color(SonaraPalette.HIGHLIGHT),
    val onHighlight: Color = Color(SonaraPalette.ON_HIGHLIGHT),
    val progress: Color = Color(SonaraPalette.PROGRESS),
    val warning: Color = Color(SonaraPalette.WARNING),
    val disabledContent: Color = Color(SonaraPalette.DISABLED_CONTENT),
    val disabledContainer: Color = Color(SonaraPalette.DISABLED_CONTAINER),
) {
    /** Divider and rule colour. Same value as [borderMuted]; named for what it is used for. */
    val divider: Color get() = borderMuted

    /** Background of a selected row. */
    val selected: Color get() = surfaceRaised

    /** Playback progress track (the unfilled part). */
    val progressTrack: Color get() = borderMuted
}

/** The palette with the chosen accent. Everything that is not the accent pair is fixed. */
fun SonaraColors.withAccent(choice: AccentChoice): SonaraColors =
    if (choice == AccentChoice.default) this else copy(accent = Color(choice.argb), onAccent = Color(choice.onArgb))
