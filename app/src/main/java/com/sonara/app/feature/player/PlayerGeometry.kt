package com.sonara.app.feature.player

/**
 * Where every element of the player sits, as pixel rectangles, in the mini-player and in the Full Player.
 * The surface that expands from one to the other is laid out by interpolating these rectangles, so the
 * geometry lives here as plain numbers: no Compose, no allocation per frame, testable on the JVM.
 *
 * A rectangle is four floats (left, top, right, bottom) at index `slot.ordinal * 4` of a [FloatArray].
 */
internal enum class PlayerSlot {
    // Shared with the mini-player: these elements travel.
    Art, Title, Artist, Play, Next,

    // Full Player only: they stay where they are and are revealed.
    Album, Favourite, Previous, Shuffle, Repeat, Seek, TimeStart, TimeEnd, Collapse, Queue, Label;

    companion object {
        val count: Int = entries.size
        val sharedCount: Int = 5
    }
}

internal class PlayerSlots(
    /** Mini-player rectangles. Slots that exist only in the Full Player repeat their Full Player rectangle. */
    val mini: FloatArray,
    val full: FloatArray,
    /** Bounds of the mini-player surface (left, top, right, bottom). */
    val surfaceMini: FloatArray,
    /** Bounds of the Full Player surface: the whole window. */
    val surfaceFull: FloatArray,
    val landscape: Boolean,
)

internal object PlayerGeometry {

    // dp values
    const val PAD = 24f
    const val EDGE = 12f
    const val TOP_BAR = 48f
    const val PLAY_FULL = 72f
    const val BUTTON = 48f
    const val MINI_ART = 40f
    const val MINI_BORDER = 1f
    const val TITLE_LINE = 34f
    const val ARTIST_LINE = 22f
    const val ALBUM_LINE = 20f
    const val SEEK_HEIGHT = 40f
    const val TIME_HEIGHT = 20f
    const val MIN_ART = 96f

    /** Type sizes (sp). The Full Player lays text out at its own size and scales it down to the mini-player's. */
    const val FULL_TITLE_SP = 28f
    const val MINI_TITLE_SP = 16f
    const val FULL_ARTIST_SP = 16f
    const val MINI_ARTIST_SP = 12f
    const val MINI_TITLE_SCALE = MINI_TITLE_SP / FULL_TITLE_SP
    const val MINI_ARTIST_SCALE = MINI_ARTIST_SP / FULL_ARTIST_SP

    /** How far (dp) a revealed control travels while it appears. */
    const val REVEAL_SHIFT = 12f

    /** Mini title line and artist line, in dp, as laid out by the mini-player. */
    const val MINI_TITLE_LINE = 22f
    const val MINI_ARTIST_LINE = 16f

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** 0..1 progress of a window that starts at [start] and lasts [length] of the overall progress [e]. */
    fun phase(e: Float, start: Float, length: Float): Float =
        if (length <= 0f) (if (e >= start) 1f else 0f) else ((e - start) / length).coerceIn(0f, 1f)

    /** Reveal windows of the Full Player extras: top bar first, then info, seek bar, transport. Ends before 1. */
    fun revealStart(slot: PlayerSlot): Float = when (slot) {
        PlayerSlot.Collapse, PlayerSlot.Label, PlayerSlot.Queue -> 0.42f
        PlayerSlot.Album, PlayerSlot.Favourite -> 0.48f
        PlayerSlot.Seek, PlayerSlot.TimeStart, PlayerSlot.TimeEnd -> 0.54f
        PlayerSlot.Previous -> 0.58f
        PlayerSlot.Shuffle, PlayerSlot.Repeat -> 0.62f
        else -> 0f
    }

    const val REVEAL_LENGTH = 0.3f

    /** Fraction 0..1 of a touch at [x] on a track [width] wide. */
    fun seekFraction(x: Float, width: Float): Float = if (width <= 0f) 0f else (x / width).coerceIn(0f, 1f)

    /** The playback position a fraction of the track stands for. Unknown or zero durations give 0. */
    fun positionForFraction(fraction: Float, durationMs: Long): Long =
        if (durationMs <= 0L) 0L else (fraction.coerceIn(0f, 1f) * durationMs).toLong().coerceIn(0L, durationMs)

    /** Fraction of the track played. */
    fun fractionOf(positionMs: Long, durationMs: Long): Float =
        if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    /**
     * @param miniBounds the mini-player's real bounds in the window (left, top, right, bottom), or null when
     *        they are not known yet; then a mini-player resting above the bottom edge is assumed.
     */
    fun compute(
        width: Float,
        height: Float,
        insetTop: Float,
        insetBottom: Float,
        density: Float,
        miniBounds: FloatArray?,
    ): PlayerSlots {
        val d = density
        val full = FloatArray(PlayerSlot.count * 4)
        val mini = FloatArray(PlayerSlot.count * 4)
        fun set(a: FloatArray, slot: PlayerSlot, l: Float, t: Float, r: Float, b: Float) {
            val i = slot.ordinal * 4
            a[i] = l; a[i + 1] = t; a[i + 2] = r; a[i + 3] = b
        }

        val landscape = width > height * 1.1f
        val columnLeft: Float
        val columnRight: Float
        if (landscape) {
            columnLeft = width * 0.46f
            columnRight = width - PAD * d
        } else {
            columnLeft = PAD * d
            columnRight = width - PAD * d
        }
        val cx = (columnLeft + columnRight) / 2f

        // Top bar
        val barTop = insetTop
        set(full, PlayerSlot.Collapse, EDGE * d, barTop, (EDGE + BUTTON) * d, barTop + BUTTON * d)
        set(full, PlayerSlot.Queue, width - (EDGE + BUTTON) * d, barTop, width - EDGE * d, barTop + BUTTON * d)
        set(full, PlayerSlot.Label, (EDGE + BUTTON + 8f) * d, barTop, width - (EDGE + BUTTON + 8f) * d, barTop + BUTTON * d)

        // Bottom-up: transport, times, seek, info.
        val transportTop = height - insetBottom - (PAD + PLAY_FULL) * d
        val transportCy = transportTop + PLAY_FULL * d / 2f
        set(full, PlayerSlot.Play, cx - PLAY_FULL * d / 2f, transportTop, cx + PLAY_FULL * d / 2f, transportTop + PLAY_FULL * d)
        val sideGap = 16f * d
        set(full, PlayerSlot.Previous, cx - PLAY_FULL * d / 2f - sideGap - BUTTON * d, transportCy - BUTTON * d / 2f, cx - PLAY_FULL * d / 2f - sideGap, transportCy + BUTTON * d / 2f)
        set(full, PlayerSlot.Next, cx + PLAY_FULL * d / 2f + sideGap, transportCy - BUTTON * d / 2f, cx + PLAY_FULL * d / 2f + sideGap + BUTTON * d, transportCy + BUTTON * d / 2f)
        set(full, PlayerSlot.Shuffle, columnLeft - EDGE * d, transportCy - BUTTON * d / 2f, columnLeft + (BUTTON - EDGE) * d, transportCy + BUTTON * d / 2f)
        set(full, PlayerSlot.Repeat, columnRight - (BUTTON - EDGE) * d, transportCy - BUTTON * d / 2f, columnRight + EDGE * d, transportCy + BUTTON * d / 2f)

        val timesTop = transportTop - 12f * d - TIME_HEIGHT * d
        set(full, PlayerSlot.TimeStart, columnLeft, timesTop, columnLeft + 80f * d, timesTop + TIME_HEIGHT * d)
        set(full, PlayerSlot.TimeEnd, columnRight - 80f * d, timesTop, columnRight, timesTop + TIME_HEIGHT * d)
        val seekTop = timesTop - SEEK_HEIGHT * d
        set(full, PlayerSlot.Seek, columnLeft, seekTop, columnRight, seekTop + SEEK_HEIGHT * d)

        val albumTop = seekTop - 12f * d - ALBUM_LINE * d
        set(full, PlayerSlot.Album, columnLeft, albumTop, columnRight, albumTop + ALBUM_LINE * d)
        val artistTop = albumTop - ARTIST_LINE * d - 2f * d
        set(full, PlayerSlot.Artist, columnLeft, artistTop, columnRight, artistTop + ARTIST_LINE * d)
        val titleTop = artistTop - 8f * d - TITLE_LINE * d
        val favRight = columnRight + EDGE * d
        set(full, PlayerSlot.Title, columnLeft, titleTop, favRight - BUTTON * d - 8f * d, titleTop + TITLE_LINE * d)
        set(full, PlayerSlot.Favourite, favRight - BUTTON * d, titleTop + TITLE_LINE * d / 2f - BUTTON * d / 2f, favRight, titleTop + TITLE_LINE * d / 2f + BUTTON * d / 2f)

        // Artwork: a square that fills what the rest leaves.
        val artTop = insetTop + (BUTTON + 12f) * d
        if (landscape) {
            val avail = height - insetBottom - PAD * d - artTop
            val size = minOf(avail, columnLeft - 2f * PAD * d).coerceAtLeast(MIN_ART * d)
            set(full, PlayerSlot.Art, PAD * d, artTop, PAD * d + size, artTop + size)
        } else {
            val maxWide = width - 2f * PAD * d
            val maxHigh = titleTop - 16f * d - artTop
            val size = minOf(maxWide, maxHigh).coerceAtLeast(minOf(MIN_ART * d, maxWide))
            val left = (width - size) / 2f
            set(full, PlayerSlot.Art, left, artTop, left + size, artTop + size)
        }

        // Mini-player
        val bounds = miniBounds ?: run {
            val miniHeight = (MINI_BORDER + 56f) * d
            val bottom = height - insetBottom - 64f * d
            floatArrayOf(0f, bottom - miniHeight, width, bottom)
        }
        val ml = bounds[0]
        val mt = bounds[1]
        val mr = bounds[2]
        val mb = bounds[3]
        val cy = (mt + MINI_BORDER * d + mb) / 2f
        for (i in full.indices) mini[i] = full[i]
        set(mini, PlayerSlot.Art, ml + 12f * d, cy - MINI_ART * d / 2f, ml + (12f + MINI_ART) * d, cy + MINI_ART * d / 2f)
        set(mini, PlayerSlot.Next, mr - 52f * d, cy - BUTTON * d / 2f, mr - 4f * d, cy + BUTTON * d / 2f)
        set(mini, PlayerSlot.Play, mr - 100f * d, cy - BUTTON * d / 2f, mr - 52f * d, cy + BUTTON * d / 2f)
        val textTop = cy - (MINI_TITLE_LINE + MINI_ARTIST_LINE) * d / 2f
        set(mini, PlayerSlot.Title, ml + 64f * d, textTop, mr - 112f * d, textTop + MINI_TITLE_LINE * d)
        set(mini, PlayerSlot.Artist, ml + 64f * d, textTop + MINI_TITLE_LINE * d, mr - 112f * d, textTop + (MINI_TITLE_LINE + MINI_ARTIST_LINE) * d)

        return PlayerSlots(
            mini = mini,
            full = full,
            surfaceMini = floatArrayOf(ml, mt, mr, mb),
            surfaceFull = floatArrayOf(0f, 0f, width, height),
            landscape = landscape,
        )
    }
}
