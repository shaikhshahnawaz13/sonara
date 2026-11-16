package com.sonora.music

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** A round button that sits raised when off and pressed-in with an accent icon when on. */
class ToggleButton(ctx: Context, iconRes: Int, desc: String, sizeDp: Float, onClick: () -> Unit) : FrameLayout(ctx) {
    private val raised = context.neo(NeoStyle.RAISED, NeoLevel.SUBTLE, -1f)
    private val pressedIn = NeoDrawable(context.resources.displayMetrics.density, NeoStyle.INSET, NeoLevel.SUBTLE, -1f, 0, 8f)
    private val icon = context.iconView(iconRes, Theme.p.textSecondary)

    init {
        val total = context.dp(sizeDp) + 2 * raised.margin
        layoutParams = LinearLayout.LayoutParams(total, total)
        val px = context.dp(sizeDp * 0.5f)
        addView(icon, LayoutParams(px, px, Gravity.CENTER))
        contentDescription = desc
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
        pressScale()
        setActive(false)
    }

    fun setActive(active: Boolean) {
        background = if (active) pressedIn else raised
        icon.setColorFilter(if (active) Theme.p.accentText else Theme.p.textSecondary)
    }

    fun setIcon(res: Int) { icon.setImageResource(res) }
}

/** Full-screen now-playing view. It slides over the app shell and is driven by PlaybackHub. */
class PlayerScreen(private val host: Host) {
    private val c = host.ctx
    private val pal = Theme.p
    private val handler = Handler(Looper.getMainLooper())
    val view = LinearLayout(c)

    private val art = ArtworkView(c, 24f)
    private val title = c.tv("", 24f, pal.text, W_BOLD).oneLine()
    private val artist = c.tv("", 16f, pal.textSecondary).oneLine()
    private val heart: FrameLayout
    private val seek = NeoSeekBar(c)
    private val elapsed = c.tv("0:00", 13f, pal.textMuted)
    private val total = c.tv("0:00", 13f, pal.textMuted)
    private val play: FrameLayout
    private val shuffle: ToggleButton
    private val repeat: ToggleButton
    private val sleepChip: TextView
    private var artPx = c.dp(320)
    private var showing = false
    private var lastTrackId = -1L

    private val tick = object : Runnable {
        override fun run() {
            updateProgress()
            if (showing) handler.postDelayed(this, 500)
        }
    }

    init {
        view.orientation = LinearLayout.VERTICAL
        view.setBackgroundColor(pal.bg)
        view.isClickable = true
        view.setPadding(0, 0, 0, 0)

        // top bar
        val top = LinearLayout(c)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(c.roundButton(R.drawable.ic_chevron_down, "Close player", 44f, NeoLevel.SUBTLE, pal.text) { host.closePlayer() })
        val kicker = c.tv("NOW PLAYING", 12f, pal.accentText, W_BOLD)
        kicker.letterSpacing = 0.16f
        kicker.gravity = Gravity.CENTER
        top.addView(kicker, LinearLayout.LayoutParams(0, WRAP, 1f))
        val spacer = View(c)
        top.addView(spacer, LinearLayout.LayoutParams(c.dp(44) + 2 * c.dp(8), 1))
        val topLp = LinearLayout.LayoutParams(MATCH, WRAP)
        topLp.leftMargin = c.dp(12)
        topLp.rightMargin = c.dp(12)
        view.addView(top, topLp)

        // artwork
        val square = FitSquare(c)
        val frame = FrameLayout(c)
        val fd = c.neo(NeoStyle.RAISED, NeoLevel.PROMINENT, 32f)
        frame.applyNeo(fd, c.dp(14), c.dp(14), c.dp(14), c.dp(14))
        frame.addView(art, FrameLayout.LayoutParams(MATCH, MATCH))
        square.addView(frame)
        val sqLp = LinearLayout.LayoutParams(MATCH, 0, 1f)
        sqLp.leftMargin = c.dp(14)
        sqLp.rightMargin = c.dp(14)
        sqLp.topMargin = c.dp(4)
        view.addView(square, sqLp)

        // title row
        val meta = LinearLayout(c)
        meta.orientation = LinearLayout.HORIZONTAL
        meta.gravity = Gravity.CENTER_VERTICAL
        val texts = LinearLayout(c)
        texts.orientation = LinearLayout.VERTICAL
        texts.addView(title)
        artist.setPadding(0, c.dp(4), 0, 0)
        texts.addView(artist)
        meta.addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f))
        heart = c.roundButton(R.drawable.ic_heart_outline, "Like", 44f, NeoLevel.SUBTLE, pal.text) {
            PlaybackHub.current?.let { host.toggleLike(it) }
        }
        val hl = heart.layoutParams as LinearLayout.LayoutParams
        hl.rightMargin = -heart.neoMargin()
        meta.addView(heart, hl)
        val metaLp = LinearLayout.LayoutParams(MATCH, WRAP)
        metaLp.leftMargin = c.dp(24)
        metaLp.rightMargin = c.dp(24)
        metaLp.topMargin = c.dp(8)
        view.addView(meta, metaLp)

        // seek
        seek.onScrub = { v -> elapsed.text = formatTime(v) }
        seek.onCommit = { v -> PlaybackHub.seekTo(v) }
        val seekLp = LinearLayout.LayoutParams(MATCH, WRAP)
        seekLp.leftMargin = c.dp(12)
        seekLp.rightMargin = c.dp(12)
        seekLp.topMargin = c.dp(8)
        view.addView(seek, seekLp)
        val times = LinearLayout(c)
        times.orientation = LinearLayout.HORIZONTAL
        times.addView(elapsed, LinearLayout.LayoutParams(0, WRAP, 1f))
        times.addView(total)
        val tlp = LinearLayout.LayoutParams(MATCH, WRAP)
        tlp.leftMargin = c.dp(24)
        tlp.rightMargin = c.dp(24)
        view.addView(times, tlp)

        // transport
        shuffle = ToggleButton(c, R.drawable.ic_shuffle, "Shuffle", 40f) { PlaybackHub.setShuffle(!PlaybackHub.shuffle) }
        repeat = ToggleButton(c, R.drawable.ic_repeat, "Repeat", 40f) { PlaybackHub.cycleRepeat() }
        val prev = c.roundButton(R.drawable.ic_skip_previous, "Previous", 52f, NeoLevel.SUBTLE, pal.text) { PlaybackHub.previous(c) }
        val next = c.roundButton(R.drawable.ic_skip_next, "Next", 52f, NeoLevel.SUBTLE, pal.text) { PlaybackHub.next(c) }
        play = c.roundButton(R.drawable.ic_play, "Play", 68f, NeoLevel.STANDARD, pal.onAccent, pal.accent) { PlaybackHub.toggle(c) }
        val controls = LinearLayout(c)
        controls.orientation = LinearLayout.HORIZONTAL
        controls.gravity = Gravity.CENTER
        val overlap = -c.dp(8)
        controls.addView(shuffle)
        for (b in listOf<View>(prev, play, next, repeat)) {
            val lp = LinearLayout.LayoutParams(b.layoutParams.width, b.layoutParams.height)
            lp.leftMargin = overlap
            controls.addView(b, lp)
        }
        val cLp = LinearLayout.LayoutParams(MATCH, WRAP)
        cLp.topMargin = c.dp(8)
        view.addView(controls, cLp)

        // extras
        val extras = LinearLayout(c)
        extras.orientation = LinearLayout.HORIZONTAL
        extras.gravity = Gravity.CENTER
        val eq = c.chip("Equalizer", false) { host.openEqualizer() }
        sleepChip = c.chip("Sleep timer", false) { host.showSleepTimer() }
        extras.addView(eq)
        val sl = LinearLayout.LayoutParams(WRAP, WRAP)
        sl.leftMargin = c.dp(4)
        extras.addView(sleepChip, sl)
        val eLp = LinearLayout.LayoutParams(MATCH, WRAP)
        eLp.topMargin = c.dp(4)
        eLp.bottomMargin = c.dp(8)
        view.addView(extras, eLp)
    }

    fun onShown() {
        showing = true
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    fun onHidden() {
        showing = false
        handler.removeCallbacks(tick)
    }

    fun refresh() {
        val t = PlaybackHub.current ?: return
        title.text = t.title
        artist.text = "${t.artist} · ${t.album}"
        if (t.id != lastTrackId) {
            lastTrackId = t.id
            Artwork.bind(art, t, artPx)
            seek.setProgress(0)
        }
        val playing = PlaybackHub.state == PlayState.PLAYING || PlaybackHub.state == PlayState.PREPARING
        play.buttonIcon().setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        play.contentDescription = if (playing) "Pause" else "Play"
        val liked = Store.isLiked(t.id)
        heart.buttonIcon().setImageResource(if (liked) R.drawable.ic_heart else R.drawable.ic_heart_outline)
        heart.buttonIcon().setColorFilter(if (liked) pal.accentText else pal.text)
        heart.contentDescription = if (liked) "Remove from liked songs" else "Add to liked songs"
        shuffle.setActive(PlaybackHub.shuffle)
        repeat.setActive(PlaybackHub.repeat != RepeatMode.OFF)
        repeat.setIcon(if (PlaybackHub.repeat == RepeatMode.ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat)
        repeat.contentDescription = when (PlaybackHub.repeat) {
            RepeatMode.OFF -> "Repeat off"
            RepeatMode.ALL -> "Repeat all"
            RepeatMode.ONE -> "Repeat one"
        }
        val left = PlaybackHub.sleepEndsAt - SystemClock.elapsedRealtime()
        sleepChip.text = if (PlaybackHub.sleepEndsAt > 0L && left > 0) "Sleep in ${(left / 60000L) + 1} min" else "Sleep timer"
        updateProgress()
    }

    private fun updateProgress() {
        val d = PlaybackHub.durationMs()
        seek.max = if (d > 0) d else 1
        val p = PlaybackHub.positionMs()
        seek.setProgress(p)
        elapsed.text = formatTime(p)
        total.text = formatTime(d)
    }
}
