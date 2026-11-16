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
            item.addView(label)
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
            a.duration = 260
            a.interpolator = android.view.animation.DecelerateInterpolator(1.6f)
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
        pill.setBounds(left.toInt(), paddingTop, (left + slot).toInt(), height - paddingBottom)
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
    private val artPx = context.dp(44)
    private val art = ArtworkView(ctx, 12f)
    private val title = context.tv("", 15f, Theme.p.text, W_MEDIUM).oneLine()
    private val artist = context.tv("", 13f, Theme.p.textSecondary).oneLine()
    private val play: FrameLayout
    private val progress = MiniProgress(ctx)

    init {
        orientation = VERTICAL
        applyNeo(context.neo(NeoStyle.RAISED, NeoLevel.STANDARD, 26f), context.dp(12), context.dp(10), context.dp(8), context.dp(12))
        val row = LinearLayout(ctx)
        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(art, LayoutParams(artPx, artPx))
        val texts = LinearLayout(ctx)
        texts.orientation = VERTICAL
        texts.addView(title)
        artist.setPadding(0, context.dp(2), 0, 0)
        texts.addView(artist)
        val tl = LayoutParams(0, WRAP, 1f)
        tl.leftMargin = context.dp(12)
        row.addView(texts, tl)
        play = context.roundButton(R.drawable.ic_play, "Play or pause", 40f, NeoLevel.SUBTLE, Theme.p.onAccent, Theme.p.accent) {
            PlaybackHub.toggle(context)
        }
        val pl = play.layoutParams as LayoutParams
        pl.rightMargin = -play.neoMargin() + context.dp(4)
        pl.topMargin = -play.neoMargin()
        pl.bottomMargin = -play.neoMargin()
        row.addView(play, pl)
        addView(row, LayoutParams(MATCH, WRAP))
        val pr = LayoutParams(MATCH, context.dp(4))
        pr.topMargin = context.dp(8)
        pr.rightMargin = context.dp(4)
        addView(progress, pr)
        isClickable = true
        isFocusable = true
        contentDescription = "Open player"
        setOnClickListener { host.openPlayer() }
    }

    fun bind() {
        val t = PlaybackHub.current ?: return
        title.text = t.title
        artist.text = t.artist
        Artwork.bind(art, t, artPx)
        val playing = PlaybackHub.state == PlayState.PLAYING || PlaybackHub.state == PlayState.PREPARING
        play.buttonIcon().setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        play.contentDescription = if (playing) "Pause" else "Play"
        updateProgress()
    }

    fun updateProgress() {
        val d = PlaybackHub.durationMs()
        progress.set(if (d > 0) PlaybackHub.positionMs().toFloat() / d else 0f)
    }
}
