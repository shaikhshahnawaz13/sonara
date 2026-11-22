package com.sonara.app.core.design

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * The accent colours a person can pick (Settings > Appearance). A controlled set rather than a free picker:
 * every entry has been checked to stay readable as text on the dark surfaces and to carry the dark
 * [onArgb] text on top of it (see AccentChoiceTest). Acid lime stays the default and the brand colour.
 *
 * Plain ARGB longs, like [SonaraPalette], so the set can be unit-tested without Compose types.
 */
enum class AccentChoice(val id: String, val label: String, val argb: Long, val onArgb: Long = SonaraPalette.ON_ACCENT) {
    Lime("lime", "Acid lime", SonaraPalette.ACCENT),
    Cyan("cyan", "Electric cyan", 0xFF00E5FFL),
    Orange("orange", "Signal orange", 0xFFFF6A00L),
    Pink("pink", "Hot pink", 0xFFFF3D9AL),
    Ice("ice", "Ice white", 0xFFBFE3FFL),
    Amber("amber", "Amber", 0xFFFFB300L);

    val color: Color get() = Color(argb)

    companion object {
        val default: AccentChoice = Lime

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
