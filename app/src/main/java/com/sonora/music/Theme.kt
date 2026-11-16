package com.sonora.music

import android.content.Context
import android.graphics.Color

/** One complete colour set. Neo-morphism is built from a single base colour plus a light and a dark shadow. */
class Palette(
    val dark: Boolean,
    val bg: Int,
    val light: Int,
    val shadow: Int,
    val surfaceTop: Int,
    val surfaceBottom: Int,
    val text: Int,
    val textSecondary: Int,
    val textMuted: Int,
    val accent: Int,
    val onAccent: Int,
    val accentText: Int
)

object Theme {
    private val darkPalette = Palette(
        dark = true,
        bg = 0xFF151C25.toInt(),
        light = 0xFF2A3644.toInt(),
        shadow = 0xFF0A0E13.toInt(),
        surfaceTop = 0xFF1A232E.toInt(),
        surfaceBottom = 0xFF131A22.toInt(),
        text = 0xFFEAF1F7.toInt(),
        textSecondary = 0xFFA9B6C3.toInt(),
        textMuted = 0xFF8393A3.toInt(),
        accent = 0xFF19D9F5.toInt(),
        onAccent = 0xFF04222B.toInt(),
        accentText = 0xFF2BE0FA.toInt()
    )
    private val lightPalette = Palette(
        dark = false,
        bg = 0xFFE4E9F1.toInt(),
        light = 0xFFFFFFFF.toInt(),
        shadow = 0xFFB3BECF.toInt(),
        surfaceTop = 0xFFE9EEF5.toInt(),
        surfaceBottom = 0xFFDDE4ED.toInt(),
        text = 0xFF1B2634.toInt(),
        textSecondary = 0xFF465669.toInt(),
        textMuted = 0xFF5B6B7F.toInt(),
        accent = 0xFF0FC3DF.toInt(),
        onAccent = 0xFF03242C.toInt(),
        accentText = 0xFF007C93.toInt()
    )

    var p: Palette = darkPalette
        private set

    fun init(ctx: Context) {
        val dark = ctx.applicationContext.getSharedPreferences("sonara_theme", Context.MODE_PRIVATE).getBoolean("dark", true)
        p = if (dark) darkPalette else lightPalette
    }

    fun setDark(ctx: Context, dark: Boolean) {
        ctx.applicationContext.getSharedPreferences("sonara_theme", Context.MODE_PRIVATE).edit().putBoolean("dark", dark).apply()
        p = if (dark) darkPalette else lightPalette
    }

    fun blend(a: Int, b: Int, t: Float): Int {
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt()
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt()
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
        return Color.rgb(r, g, bl)
    }
}
