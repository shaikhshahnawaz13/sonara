package com.sonara.app.core.design

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * The accent colours a person can pick (Settings > Appearance). In V2 the accent is the interactive colour: primary
 * buttons, text actions and the active destination. It is therefore a controlled set of deep, calm tones rather than
 * a free picker: every entry has been checked to stay readable as text on the ivory surfaces and to carry the ivory
 * [onArgb] text on top of it (see AccentChoiceTest). Forest green is the default and the brand colour. Coral is not
 * offered: it is the signal colour and too light to be text.
 *
 * Plain ARGB longs, like [SonaraPalette], so the set can be unit-tested without Compose types.
 * Ids saved by V1 ("lime", "cyan", ...) are unknown to V2 and fall back to the default.
 */
enum class AccentChoice(val id: String, val label: String, val argb: Long, val onArgb: Long = SonaraPalette.ON_ACCENT) {
    Forest("forest", "Forest green", SonaraPalette.ACCENT),
    Cobalt("cobalt", "Cobalt", 0xFF1F3F8FL),
    Brick("brick", "Brick", 0xFF8C3B2AL),
    Plum("plum", "Plum", 0xFF5E2B63L),
    Umber("umber", "Umber", 0xFF6A4A1FL);

    val color: Color get() = Color(argb)

    companion object {
        val default: AccentChoice = Forest

        /** The saved id's choice; anything missing or unknown (an older or newer build) falls back to the default. */
        fun fromId(id: String?): AccentChoice = entries.firstOrNull { it.id == id } ?: default
    }
}

/** WCAG relative luminance and contrast ratio for opaque ARGB values. */
object ColorContrast {

    private fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF).toInt()
        val g = ((argb shr 8) and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    fun ratio(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}
