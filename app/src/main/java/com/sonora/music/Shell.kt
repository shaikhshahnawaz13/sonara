package com.sonora.music

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

const val SLIDE_MS = 280L
fun slideInterpolator(): android.view.animation.Interpolator = android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f)

/** Bottom navigation: a raised bar with an inset pill that glides to the selected destination. */
class NavBar(ctx: Context, private val onSelect: (Int) -> Unit) : LinearLayout(ctx) {
    private val labels = ArrayList<TextView>()
    private val icons = ArrayList<ImageView>()
    private var selected = 0
    private var indicator = 0f
    private var animator: ValueAnimator? = null
    private val pill = context.neo(NeoStyle.INSET, NeoLevel.SUBTLE, 24f)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        applyNeo(context.neo(NeoStyle.RAISED, NeoLevel.STANDARD, 30f), context.dp(6), context.dp(6), context.dp(6), context.dp(6))
        setWillNotDraw(false)
        val defs = listOf(
            Pair("Home", R.drawable.ic_home),
            Pair("Search", R.drawable.ic_search),
            Pair("Library", R.drawable.ic_library)
        )
        defs.forEachIndexed { i, def ->
            val item = LinearLayout(ctx)
            item.orientation = VERTICAL
            item.gravity = Gravity.CENTER
            item.setPadding(0, context.dp(9), 0, context.dp(8))
            item.isClickable = true
            item.isFocusable = true
            item.contentDescription = def.first
            item.setOnClickListener { onSelect(i) }
            val icon = context.iconView(def.second, Theme.p.textMuted)
            item.addView(icon, LayoutParams(context.dp(24), context.dp(24)))
            val label = context.tv(def.first, 12f, Theme.p.textMuted, W_MEDIUM)
            label.setPadding(0, context.dp(3), 0, 0)
            label.gravity = Gravity.CENTER
            item.addView(label, LayoutParams(WRAP, WRAP))
            icons.add(icon)
            labels.add(label)
            addView(item, LayoutParams(0, WRAP, 1f))
        }
        applyColors()
    }

    fun select(index: Int, animate: Boolean) {
        if (index == selected && animator == null) { applyColors(); return }
        selected = index
        applyColors()
        animator?.cancel()
        if (animate && context.animationsEnabled() && isAttachedToWindow) {
            val a = ValueAnimator.ofFloat(indicator, index.toFloat())
            a.duration = SLIDE_MS
            a.interpolator = slideInterpolator()
            a.addUpdateListener { indicator = it.animatedValue as Float; invalidate() }
            animator = a
            a.start()
        } else {
            indicator = index.toFloat()
            invalidate()
        }
    }

    private fun applyColors() {
        for (i in icons.indices) {
            val c = if (i == selected) Theme.p.accentText else Theme.p.textMuted
            icons[i].setColorFilter(c)
            labels[i].setTextColor(c)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inner = width - paddingLeft - paddingRight
        val slot = inner / 3f
        val left = paddingLeft + slot * indicator
        val gap = context.dp(3)
        pill.setBounds((left + gap).toInt(), paddingTop, (left + slot - gap).toInt(), height - paddingBottom)
        pill.draw(canvas)
    }
}

/** Slim progress line used by the mini-player. */
class MiniProgress(ctx: Context) : View(ctx) {
    private var frac = 0f
    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        track.color = Theme.blend(Theme.p.bg, Theme.p.shadow, 0.45f)
        fill.color = Theme.p.accent
    }

    fun set(f: Float) {
        val v = f.coerceIn(0f, 1f)
        if (v != frac) { frac = v; invalidate() }
    }

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat()
        rect.set(0f, 0f, width.toFloat(), h)
        canvas.drawRoundRect(rect, h / 2f, h / 2f, track)
        if (frac > 0f) {
            rect.set(0f, 0f, maxOf(h, width * frac), h)
            canvas.drawRoundRect(rect, h / 2f, h / 2f, fill)
        }
    }
}

class MiniPlayer(ctx: Context, private val host: Host) : LinearLayout(ctx) {
    private val ringPx = context.dp(52)
    val ring = RingArt(ctx, false)
    val title = context.tv("", 15f, Theme.p.text, W_MEDIUM).oneLine()
    val artist = context.tv("", 12.5f, Theme.p.textSecondary).oneLine()
    val seek = SlimSeek(ctx)
    val prev: FrameLayout
    val play: FrameLayout
    val next: FrameLayout

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        applyNeo(context.neo(NeoStyle.RAISED, NeoLevel.STANDARD, 30f), context.dp(14), context.dp(10), context.dp(10), context.dp(10))
        val rm = ring.plateMargin
        addAligned(ring, ringPx + 2 * rm, ringPx + 2 * rm)

        val divider = View(ctx)
        divider.setBackgroundColor(Theme.blend(Theme.p.bg, Theme.p.shadow, 0.3f))
        val dl = LayoutParams(context.dp(1), context.dp(34))
        dl.leftMargin = context.dp(10)
        dl.rightMargin = context.dp(10)
        addView(divider, dl)

        val texts = LinearLayout(ctx)
        texts.orientation = VERTICAL
        texts.addView(title)
        artist.setPadding(0, context.dp(1), 0, 0)
        texts.addView(artist)
        val sl = LayoutParams(MATCH, context.dp(22))
        sl.leftMargin = -context.dp(9)
        sl.rightMargin = -context.dp(9)
        sl.topMargin = context.dp(2)
        texts.addView(seek, sl)
        addView(texts, LayoutParams(0, WRAP, 1f))

        prev = context.roundButton(R.drawable.ic_skip_previous, "Previous", 34f, NeoLevel.SUBTLE, Theme.p.text) { PlaybackHub.previous(context) }
        play = context.roundButton(R.drawable.ic_play, "Play or pause", 42f, NeoLevel.SUBTLE, Theme.p.onAccent, Theme.p.accent) { PlaybackHub.toggle(context) }
        next = context.roundButton(R.drawable.ic_skip_next, "Next", 34f, NeoLevel.SUBTLE, Theme.p.text) { PlaybackHub.next(context) }
        addAligned(prev, prev.layoutParams.width, prev.layoutParams.height, context.dp(6), 0, context.dp(2), 0)
        addAligned(play, play.layoutParams.width, play.layoutParams.height, 0, 0, context.dp(2), 0)
        addAligned(next, next.layoutParams.width, next.layoutParams.height, 0, 0, context.dp(2), 0)

        seek.onCommit = { v -> PlaybackHub.seekTo(v) }
        isClickable = true
        isFocusable = true
        contentDescription = "Open player"
        setOnClickListener { host.openPlayer() }
        unclip()
    }

    fun bind() {
        val t = PlaybackHub.current ?: return
        title.text = t.title
        artist.text = t.artist
        ring.bind(t, ringPx)
        val playing = PlaybackHub.state == PlayState.PLAYING || PlaybackHub.state == PlayState.PREPARING
        play.buttonIcon().setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        play.contentDescription = if (playing) "Pause" else "Play"
        updateProgress()
    }

    fun updateProgress() {
        val d = PlaybackHub.durationMs()
        seek.max = if (d > 0) d else 1
        val p = PlaybackHub.positionMs()
        seek.set(p)
        ring.setFraction(if (d > 0) p.toFloat() / d else 0f)
    }
}
