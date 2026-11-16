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
    private val pressedIn = NeoDrawable(context.resources.displayMetrics.density, NeoStyle.INSET, NeoLevel.SUBTLE, -1f, 0, 12f)
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

/** Full-screen now-playing view. MainActivity morphs the mini-player into it; PlaybackHub drives it. */
class PlayerScreen(private val host: Host) {
    private val c = host.ctx
    private val pal = Theme.p
    private val handler = Handler(Looper.getMainLooper())
    val view = LinearLayout(c)

    val ring = RingArt(c, true)
    val title = c.tv("", 24f, pal.text, W_BOLD).oneLine()
    val artist = c.tv("", 16f, pal.textSecondary).oneLine()
    private val heart: FrameLayout
    private val seek = NeoSeekBar(c)
    private val elapsed = c.tv("0:00", 13f, pal.textMuted)
    private val total = c.tv("0:00", 13f, pal.textMuted)
    val play: FrameLayout
    val prev: FrameLayout
    val next: FrameLayout
    private val shuffle: ToggleButton
    private val repeat: ToggleButton
    private val sleepChip: TextView
    val header: View
    private val seekBlock: View
    private val extras: View
    private val dock = LinearLayout(c)
    private var ringPx = c.dp(260)
    private var showing = false
    private var lastTrackId = -1L

    /** Everything that fades in as the container expands (the shared pieces are moved by the morph instead). */
    val fadeGroup: List<View> get() = listOf(header, seekBlock, shuffle, repeat, extras)

    fun setDockAlpha(a: Float) { dock.background?.alpha = (255 * a).toInt() }

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
        view.clipChildren = false
        view.clipToPadding = false

        // header: collapse, brand, like
        val top = LinearLayout(c)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.clipChildren = false
        val close = c.roundButton(R.drawable.ic_chevron_down, "Collapse player", 44f, NeoLevel.SUBTLE, pal.text) { host.closePlayer() }
        top.addAligned(close, close.layoutParams.width, close.layoutParams.height, 0, 0, 0, 0)
        val brand = LinearLayout(c)
        brand.orientation = LinearLayout.HORIZONTAL
        brand.gravity = Gravity.CENTER
        brand.addView(c.logoMark(28f))
        val word = c.tv("SONARA", 16f, pal.text, W_BOLD)
        word.letterSpacing = 0.2f
        word.setPadding(c.dp(8), 0, 0, 0)
        brand.addView(word)
        top.addView(brand, LinearLayout.LayoutParams(0, WRAP, 1f))
        heart = c.roundButton(R.drawable.ic_heart_outline, "Like", 44f, NeoLevel.SUBTLE, pal.text) {
            PlaybackHub.current?.let { host.toggleLike(it) }
        }
        top.addAligned(heart, heart.layoutParams.width, heart.layoutParams.height, 0, 0, 0, 0)
        val topLp = LinearLayout.LayoutParams(MATCH, WRAP)
        topLp.leftMargin = c.dp(20)
        topLp.rightMargin = c.dp(20)
        topLp.topMargin = c.dp(8)
        view.addView(top, topLp)
        header = top

        // artwork module: round art with a cyan progress ring, centred in the free space
        val dm = c.resources.displayMetrics
        val visible = minOf(dm.widthPixels - c.dp(88), (dm.heightPixels * 0.40f).toInt(), c.dp(320))
        ringPx = visible
        val slot = FrameLayout(c)
        slot.clipChildren = false
        val rm = ring.plateMargin
        slot.addView(ring, FrameLayout.LayoutParams(visible + 2 * rm, visible + 2 * rm, Gravity.CENTER))
        view.addView(slot, LinearLayout.LayoutParams(MATCH, 0, 1f))

        // title block
        val texts = LinearLayout(c)
        texts.orientation = LinearLayout.VERTICAL
        texts.addView(title)
        artist.setPadding(0, c.dp(4), 0, 0)
        texts.addView(artist)
        val metaLp = LinearLayout.LayoutParams(MATCH, WRAP)
        metaLp.leftMargin = c.dp(28)
        metaLp.rightMargin = c.dp(28)
        metaLp.topMargin = c.dp(4)
        view.addView(texts, metaLp)

        // seek + times
        val sb = LinearLayout(c)
        sb.orientation = LinearLayout.VERTICAL
        sb.clipChildren = false
        seek.onScrub = { v -> elapsed.text = formatTime(v) }
        seek.onCommit = { v -> PlaybackHub.seekTo(v) }
        sb.addView(seek, LinearLayout.LayoutParams(MATCH, WRAP))
        val times = LinearLayout(c)
        times.orientation = LinearLayout.HORIZONTAL
        times.addView(elapsed, LinearLayout.LayoutParams(0, WRAP, 1f))
        times.addView(total)
        sb.addView(times, LinearLayout.LayoutParams(MATCH, WRAP).also { it.leftMargin = c.dp(16); it.rightMargin = c.dp(16) })
        view.addView(sb, LinearLayout.LayoutParams(MATCH, WRAP).also { it.leftMargin = c.dp(12); it.rightMargin = c.dp(12); it.topMargin = c.dp(10) })
        seekBlock = sb

        // control dock
        shuffle = ToggleButton(c, R.drawable.ic_shuffle, "Shuffle", 40f) { PlaybackHub.applyShuffle(!PlaybackHub.shuffle) }
        repeat = ToggleButton(c, R.drawable.ic_repeat, "Repeat", 40f) { PlaybackHub.cycleRepeat() }
        prev = c.roundButton(R.drawable.ic_skip_previous, "Previous", 48f, NeoLevel.SUBTLE, pal.text) { PlaybackHub.previous(c) }
        next = c.roundButton(R.drawable.ic_skip_next, "Next", 48f, NeoLevel.SUBTLE, pal.text) { PlaybackHub.next(c) }
        play = c.roundButton(R.drawable.ic_play, "Play", 62f, NeoLevel.STANDARD, pal.onAccent, pal.accent) { PlaybackHub.toggle(c) }
        dock.orientation = LinearLayout.HORIZONTAL
        dock.gravity = Gravity.CENTER
        dock.applyNeo(c.neo(NeoStyle.RAISED, NeoLevel.STANDARD, -1f), c.dp(8), c.dp(8), c.dp(8), c.dp(8))
        val seq = listOf<View>(shuffle, prev, play, next, repeat)
        seq.forEachIndexed { i, b ->
            val m = b.neoMargin()
            val lp = LinearLayout.LayoutParams(b.layoutParams.width, b.layoutParams.height)
            lp.leftMargin = (if (i == 0) 0 else c.dp(6)) - m
            lp.rightMargin = -m
            lp.topMargin = -m
            lp.bottomMargin = -m
            dock.addView(b, lp)
        }
        val dm2 = dock.neoMargin()
        val dl = LinearLayout.LayoutParams(MATCH, WRAP)
        dl.leftMargin = c.dp(20) - dm2
        dl.rightMargin = c.dp(20) - dm2
        dl.topMargin = c.dp(18) - dm2
        dl.bottomMargin = -dm2
        view.addView(dock, dl)

        // extras that already existed in the app
        val ex = LinearLayout(c)
        ex.orientation = LinearLayout.HORIZONTAL
        ex.gravity = Gravity.CENTER
        ex.clipChildren = false
        val eq = c.chip("Equalizer", false) { host.openEqualizer() }
        sleepChip = c.chip("Sleep timer", false) { host.showSleepTimer() }
        ex.addView(eq)
        ex.addView(sleepChip, LinearLayout.LayoutParams(WRAP, WRAP).also { it.leftMargin = c.dp(4) })
        view.addView(ex, LinearLayout.LayoutParams(MATCH, WRAP).also { it.topMargin = c.dp(4); it.bottomMargin = c.dp(10) })
        extras = ex
        view.unclip()
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
            ring.bind(t, ringPx)
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
        ring.setFraction(if (d > 0) p.toFloat() / d else 0f)
        elapsed.text = formatTime(p)
        total.text = formatTime(d)
    }
}
