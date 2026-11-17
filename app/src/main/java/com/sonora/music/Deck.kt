package com.sonora.music

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView

/** Length of the signature mini-player -> full player transformation. Change here to tune the feel. */
const val PLAYER_OPEN_MS = 5500L
const val PLAYER_CLOSE_MS = 4200L

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
private fun smooth(a: Float, b: Float, x: Float): Float {
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Circular artwork with a rim and a cyan progress arc. It re-lays itself out at any size, always a true circle. */
class DeckArt(ctx: Context) : FrameLayout(ctx) {
    private val art = ArtworkView(ctx, 600f)
    private val trackP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val oval = RectF()
    private var frac = 0f
    private var rimR = 0f
    private var half = 0f

    init {
        setWillNotDraw(false)
        clipChildren = false
        trackP.style = Paint.Style.STROKE
        trackP.strokeCap = Paint.Cap.ROUND
        trackP.color = Theme.blend(Theme.p.bg, Theme.p.shadow, 0.22f)
        arcP.style = Paint.Style.STROKE
        arcP.strokeCap = Paint.Cap.ROUND
        arcP.color = Theme.p.accent
        rimP.style = Paint.Style.FILL
        rimP.color = Theme.blend(Theme.p.bg, Theme.p.light, 0.85f)
        addView(art, LayoutParams(MATCH, MATCH))
    }

    fun bind(t: Track?, px: Int) { Artwork.bind(art, t, px) }

    fun setFraction(f: Float) {
        val v = f.coerceIn(0f, 1f)
        if (v != frac) { frac = v; invalidate() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val side = minOf(r - l, b - t).toFloat()
        half = side / 2f
        val stroke = half * 0.09f
        val arcR = half - stroke * 0.5f - half * 0.03f
        oval.set(half - arcR, half - arcR, half + arcR, half + arcR)
        trackP.strokeWidth = stroke
        arcP.strokeWidth = stroke
        val artR = half * 0.60f
        rimR = artR + half * 0.05f
        val a = (artR * 2f).toInt()
        art.measure(MeasureSpec.makeMeasureSpec(a, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(a, MeasureSpec.EXACTLY))
        val off = (half - artR).toInt()
        art.layout(off, off, off + a, off + a)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(half, half, rimR, rimP)
        canvas.drawArc(oval, 0f, 360f, false, trackP)
        if (frac > 0f) canvas.drawArc(oval, -90f, 360f * frac, false, arcP)
    }
}

/**
 * The player. One view tree, one progress value [p] (0 = mini-player, 1 = full player).
 * The sculpted surfaces, artwork ring, text, seek bar and every button are positioned from the same [p],
 * so the mini-player is physically reshaped into the full player instead of being swapped for it.
 */
class PlayerDeck(ctx: Context, private val host: Host, private val anchorProvider: () -> RectF?) : ViewGroup(ctx) {
    private val pal = Theme.p
    private val d = ctx.resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())

    val art = DeckArt(ctx)
    private val title = ctx.tv("", 20f, pal.text, W_BOLD).oneLine()
    private val artist = ctx.tv("", 15f, pal.textSecondary).oneLine()
    private val seek = SlimSeek(ctx)
    private val elapsed = ctx.tv("0:00", 13f, pal.textMuted)
    private val total = ctx.tv("0:00", 13f, pal.textMuted)
    private val brand = LinearLayout(ctx)
    private val collapse: FrameLayout
    private val menu: FrameLayout
    private val bottom: FrameLayout
    private val prev: FrameLayout
    private val play: FrameLayout
    private val next: FrameLayout
    private val shuffle: ToggleButton
    private val repeat: ToggleButton

    private val baseSize = HashMap<View, IntArray>()

    var p = 0f
        private set
    var target = 0f
        private set
    private var anim: ValueAnimator? = null
    private var anchor: RectF? = null
    private var insL = 0
    private var insT = 0
    private var insR = 0
    private var insB = 0
    private var gestureOk = false
    private var lastTrackId = -1L
    private var showing = false

    // geometry shared by layout and drawing
    private var bR = 0f
    private var bCx = 0f
    private var bCy = 0f
    private var bodyR = 0f
    private var bodyHalf = 0f
    private var podL = 0f
    private var podR = 0f
    private var podCy = 0f
    private var podHalf = 0f
    private var neckA = 0f
    private var veilA = 0f
    private var divX = 0f
    private var divHalf = 0f
    private var divA = 0f

    private val pathBulb = Path()
    private val pathBody = Path()
    private val pathPod = Path()
    private val pathNeck = Path()
    private val rect = RectF()
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val veilPaint = Paint()
    private val divPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val tick = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 400)
        }
    }

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        fillPaint.color = Theme.blend(pal.surfaceTop, pal.surfaceBottom, 0.35f)
        veilPaint.color = pal.bg
        divPaint.color = Theme.blend(pal.bg, pal.shadow, 0.30f)
        shadowPaint.style = Paint.Style.FILL_AND_STROKE
        shadowPaint.strokeJoin = Paint.Join.ROUND

        collapse = ctx.roundButton(R.drawable.ic_chevron_down, "Collapse player", 44f, NeoLevel.SUBTLE, pal.text) { collapse() }
        menu = ctx.roundButton(R.drawable.ic_more, "Player menu", 44f, NeoLevel.SUBTLE, pal.text) { showMenu() }
        bottom = ctx.roundButton(R.drawable.ic_chevron_down, "Collapse player", 44f, NeoLevel.SUBTLE, pal.textSecondary) { collapse() }
        prev = ctx.roundButton(R.drawable.ic_skip_previous, "Previous", 46f, NeoLevel.SUBTLE, pal.text) { PlaybackHub.previous(ctx) }
        play = ctx.roundButton(R.drawable.ic_play, "Play", 60f, NeoLevel.STANDARD, pal.onAccent, pal.accent) { PlaybackHub.toggle(ctx) }
        next = ctx.roundButton(R.drawable.ic_skip_next, "Next", 46f, NeoLevel.SUBTLE, pal.text) { PlaybackHub.next(ctx) }
        shuffle = ToggleButton(ctx, R.drawable.ic_shuffle, "Shuffle", 40f) { PlaybackHub.applyShuffle(!PlaybackHub.shuffle) }
        repeat = ToggleButton(ctx, R.drawable.ic_repeat, "Repeat", 40f) { PlaybackHub.cycleRepeat() }

        brand.orientation = LinearLayout.HORIZONTAL
        brand.gravity = android.view.Gravity.CENTER_VERTICAL
        brand.addView(ctx.logoMark(26f))
        val word = ctx.tv("SONARA", 16f, pal.text, W_BOLD)
        word.letterSpacing = 0.2f
        word.setPadding(ctx.dp(8), 0, 0, 0)
        brand.addView(word)

        seek.onCommit = { v -> PlaybackHub.seekTo(v) }
        title.isClickable = false

        val all = listOf<View>(art, title, artist, seek, elapsed, total, brand, collapse, menu, bottom, prev, play, next, shuffle, repeat)
        for (v in all) {
            val lp = v.layoutParams
            if (v is FrameLayout && lp != null && lp.width > 0) {
                baseSize[v] = intArrayOf(lp.width, lp.height)
                addView(v, ViewGroup.LayoutParams(lp.width, lp.height))
            } else {
                addView(v, ViewGroup.LayoutParams(WRAP, WRAP))
            }
        }
        isClickable = true
        setOnClickListener { if (target == 0f) expand() }
        contentDescription = "Open player"
        visibility = View.GONE
    }

    // ------------------------------------------------------------------ public API

    val isExpanded: Boolean get() = target > 0.5f

    fun setInsets(l: Int, t: Int, r: Int, b: Int) {
        insL = l; insT = t; insR = r; insB = b
        placeChildren()
        invalidate()
    }

    /** Called by the activity whenever the mini slot may have appeared, disappeared or moved. */
    fun syncVisibility(allowed: Boolean) {
        val show = (allowed && PlaybackHub.current != null) || p > 0f || target > 0f
        val v = if (show) View.VISIBLE else View.GONE
        if (visibility != v) {
            visibility = v
            if (show) { handler.removeCallbacks(tick); handler.post(tick) } else handler.removeCallbacks(tick)
        }
        if (show) { refresh(); if (p == 0f) { placeChildren(); invalidate() } }
    }

    fun expand() { if (PlaybackHub.current != null) animateTo(1f) }
    fun collapse() { animateTo(0f) }

    private fun animateTo(t: Float) {
        if (t == target && anim != null) return
        target = t
        if (p == 0f && t > 0f) anchor = anchorProvider()?.let { RectF(it) }
        anim?.cancel()
        if (!context.animationsEnabled()) {
            setProgress(t)
            onSettled()
            return
        }
        val dist = Math.abs(t - p)
        if (dist < 0.001f) { setProgress(t); onSettled(); return }
        val a = ValueAnimator.ofFloat(p, t)
        a.duration = ((if (t > p) PLAYER_OPEN_MS else PLAYER_CLOSE_MS) * dist).toLong().coerceAtLeast(450L)
        a.interpolator = if (t > p) PathInterpolator(0.22f, 0f, 0.1f, 1f) else PathInterpolator(0.35f, 0f, 0.15f, 1f)
        a.addUpdateListener { setProgress(it.animatedValue as Float) }
        var cancelled = false
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) { cancelled = true }
            override fun onAnimationEnd(animation: Animator) {
                if (anim === a) anim = null
                if (!cancelled) { setProgress(target); onSettled() }
            }
        })
        anim = a
        a.start()
    }

    private fun onSettled() {
        if (p == 0f) {
            anchor = null
            handler.post { (parent as? View)?.requestLayout() }
        }
        (context as? MainActivity)?.onDeckSettled()
    }

    private fun setProgress(v: Float) {
        p = v.coerceIn(0f, 1f)
        placeChildren()
        invalidate()
    }

    // ------------------------------------------------------------------ data

    fun refresh() {
        val t = PlaybackHub.current ?: return
        title.text = t.title
        artist.text = t.artist
        if (t.id != lastTrackId) {
            lastTrackId = t.id
            art.bind(t, (resources.displayMetrics.widthPixels * 0.5f).toInt())
        }
        val playing = PlaybackHub.state == PlayState.PLAYING || PlaybackHub.state == PlayState.PREPARING
        play.buttonIcon().setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        play.contentDescription = if (playing) "Pause" else "Play"
        shuffle.setActive(PlaybackHub.shuffle)
        repeat.setActive(PlaybackHub.repeat != RepeatMode.OFF)
        repeat.setIcon(if (PlaybackHub.repeat == RepeatMode.ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat)
        repeat.contentDescription = when (PlaybackHub.repeat) {
            RepeatMode.OFF -> "Repeat off"
            RepeatMode.ALL -> "Repeat all"
            RepeatMode.ONE -> "Repeat one"
        }
        updateProgress()
    }

    fun updateProgress() {
        if (visibility != View.VISIBLE) return
        val dur = PlaybackHub.durationMs()
        seek.max = if (dur > 0) dur else 1
        val pos = PlaybackHub.positionMs()
        seek.set(pos)
        art.setFraction(if (dur > 0) pos.toFloat() / dur else 0f)
        elapsed.text = formatTime(pos)
        total.text = formatTime(dur)
    }

    private fun showMenu() {
        val t = PlaybackHub.current ?: return
        val pm = PopupMenu(context, menu)
        pm.menu.add(0, 1, 0, "Equalizer")
        pm.menu.add(0, 2, 1, "Sleep timer")
        pm.menu.add(0, 3, 2, "Track options")
        pm.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> host.openEqualizer()
                2 -> host.showSleepTimer()
                3 -> host.trackMenu(menu, t, null)
            }
            true
        }
        pm.show()
    }

    // ------------------------------------------------------------------ touch: outside the mini surface, touches pass through

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            val a = anchor ?: anchorProvider()
            gestureOk = p > 0f || target > 0f || (a != null && a.contains(ev.x, ev.y))
        }
        if (!gestureOk) return false
        return super.dispatchTouchEvent(ev)
    }

    // ------------------------------------------------------------------ layout

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        placeChildren()
    }

    private fun put(v: View, cx: Float, cy: Float, scale: Float, alpha: Float) {
        val bs = baseSize[v] ?: return
        v.measure(MeasureSpec.makeMeasureSpec(bs[0], MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(bs[1], MeasureSpec.EXACTLY))
        val l = (cx - bs[0] / 2f).toInt()
        val t = (cy - bs[1] / 2f).toInt()
        v.layout(l, t, l + bs[0], t + bs[1])
        v.pivotX = bs[0] / 2f
        v.pivotY = bs[1] / 2f
        v.scaleX = scale
        v.scaleY = scale
        v.alpha = alpha
        v.visibility = if (alpha < 0.02f) View.INVISIBLE else View.VISIBLE
    }

    private fun text(v: TextView, left: Float, top: Float, visW: Float, scale: Float, alpha: Float) {
        val w = (visW / scale).toInt().coerceAtLeast(1)
        v.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        v.layout(left.toInt(), top.toInt(), left.toInt() + w, top.toInt() + v.measuredHeight)
        v.pivotX = 0f
        v.pivotY = 0f
        v.scaleX = scale
        v.scaleY = scale
        v.alpha = alpha
    }

    private fun placeChildren() {
        val W = width.toFloat()
        val H = height.toFloat()
        if (W <= 0f || H <= 0f) return
        val a = anchor ?: anchorProvider() ?: return
        val x0 = a.left
        val x1 = a.right
        val cy0 = a.centerY()
        val wv = x1 - x0
        val r0 = a.height() / 2f

        // full-player geometry
        val r1 = 0.19f * wv
        val hd1 = 92f * d
        val gap = 30f * d
        val topEdge = insT + 60f * d
        val bottomEdge = H - insB - 76f * d
        val groupH = 2f * r1 + gap + hd1
        val yTop = topEdge + ((bottomEdge - topEdge - groupH) * 0.42f).coerceAtLeast(8f * d)
        val cyA = yTop + r1
        val dockCy = yTop + 2f * r1 + gap + hd1 / 2f

        // one progress value, a few overlapping windows of it
        val s = p
        val tr = smooth(0f, 0.9f, p)
        val tt = smooth(0.04f, 0.95f, p)
        val tc = smooth(0.06f, 0.97f, p)
        val fade = smooth(0.45f, 1f, p)

        val podW0 = 120f * d
        bR = lerp(r0, r1, tr)
        bCx = lerp(x0 + r0, x0 + r1, tr)
        bCy = lerp(cy0, cyA, tr)
        bodyR = lerp(x1 - podW0 + 26f * d, x1, s)
        bodyHalf = lerp(28f * d, 0.84f * r1, s)
        podL = lerp(x1 - podW0, x0, tc)
        podR = x1
        podCy = lerp(cy0, dockCy, tc)
        podHalf = lerp(26f * d, hd1 / 2f, tc)
        neckA = 1f - smooth(0f, 0.3f, p)
        veilA = smooth(0f, 0.55f, p)
        divX = bCx + bR + 5f * d
        divHalf = bodyHalf * 0.5f
        divA = 1f

        // artwork ring
        val side = (bR * 2f)
        art.measure(MeasureSpec.makeMeasureSpec(side.toInt(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(side.toInt(), MeasureSpec.EXACTLY))
        art.layout((bCx - bR).toInt(), (bCy - bR).toInt(), (bCx + bR).toInt(), (bCy + bR).toInt())

        // text block
        val mL = x0 + 2f * r0 + 12f * d
        val fL = x0 + 2f * r1 + 14f * d
        val mW = (x1 - podW0 - 8f * d) - mL
        val fW = (x1 - 18f * d) - fL
        val tL = lerp(mL, fL, tt)
        val tW = lerp(mW, fW, tt)
        text(title, tL, lerp(cy0 - 28f * d, cyA - 52f * d, tt), tW, lerp(0.75f, 1f, tt), 1f)
        text(artist, tL, lerp(cy0 - 9f * d, cyA - 24f * d, tt), tW, lerp(0.83f, 1f, tt), 1f)
        val seekH = lerp(20f * d, 24f * d, tt)
        val seekTop = lerp(cy0 + 9f * d, cyA + 2f * d, tt)
        seek.trackDp = lerp(5f, 6f, tt)
        seek.thumbDp = lerp(7f, 9f, tt)
        seek.measure(MeasureSpec.makeMeasureSpec((tW + 18f * d).toInt(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(seekH.toInt(), MeasureSpec.EXACTLY))
        seek.layout((tL - 9f * d).toInt(), seekTop.toInt(), (tL - 9f * d + tW + 18f * d).toInt(), (seekTop + seekH).toInt())
        seek.invalidate()
        val timesTop = cyA + 30f * d
        elapsed.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        total.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        elapsed.layout(fL.toInt(), timesTop.toInt(), fL.toInt() + elapsed.measuredWidth, timesTop.toInt() + elapsed.measuredHeight)
        val tr2 = (fL + fW).toInt()
        total.layout(tr2 - total.measuredWidth, timesTop.toInt(), tr2, timesTop.toInt() + total.measuredHeight)
        val tAlpha = smooth(0.55f, 1f, p)
        elapsed.alpha = tAlpha
        total.alpha = tAlpha
        elapsed.visibility = if (tAlpha < 0.02f) View.INVISIBLE else View.VISIBLE
        total.visibility = elapsed.visibility

        // header and bottom handle
        val headCy = insT + 8f * d + 26f * d
        val up = (1f - fade) * -10f * d
        put(collapse, x0 + 22f * d, headCy + up, 1f, fade)
        put(menu, x1 - 22f * d, headCy + up, 1f, fade)
        brand.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val bl = (W / 2f - brand.measuredWidth / 2f).toInt()
        val bt = (headCy + up - brand.measuredHeight / 2f).toInt()
        brand.layout(bl, bt, bl + brand.measuredWidth, bt + brand.measuredHeight)
        brand.alpha = fade
        brand.visibility = if (fade < 0.02f) View.INVISIBLE else View.VISIBLE
        put(bottom, W / 2f, H - insB - 16f * d - 26f * d - (1f - fade) * -10f * d, 1f, fade)

        // controls ride on the pod, which becomes the dock
        val podW = podR - podL
        val sp = (wv - hd1) / 4f
        fun frac(k: Int) = (hd1 / 2f + k * sp) / wv
        val sPrev = lerp(0.61f, 1f, tc)
        val sPlay = lerp(0.63f, 1f, tc)
        put(prev, podL + podW * lerp(0.2f, frac(1), tc), podCy, sPrev, 1f)
        put(play, podL + podW * lerp(0.5f, frac(2), tc), podCy, sPlay, 1f)
        put(next, podL + podW * lerp(0.8f, frac(3), tc), podCy, sPrev, 1f)
        val extra = smooth(0.4f, 1f, p)
        put(shuffle, podL + podW * lerp(0.12f, frac(0), tc), podCy, lerp(0.6f, 1f, tc), extra)
        put(repeat, podL + podW * lerp(0.88f, frac(4), tc), podCy, lerp(0.6f, 1f, tc), extra)
    }

    // ------------------------------------------------------------------ drawing: sculpted surfaces

    private fun softShadow(c: Canvas, path: Path, dx: Float, dy: Float, color: Int, blur: Float, peak: Float) {
        val layers = 7
        c.save()
        c.translate(dx, dy)
        shadowPaint.color = color
        val each = (peak * 1.5f / layers).coerceAtMost(1f)
        shadowPaint.alpha = (each * 255f).toInt().coerceIn(0, 255)
        for (i in 0 until layers) {
            val hw = blur * (1f - i / layers.toFloat())
            shadowPaint.strokeWidth = hw * 2f
            c.drawPath(path, shadowPaint)
        }
        c.restore()
    }

    private fun buildPaths() {
        pathBulb.rewind()
        pathBulb.addCircle(bCx, bCy, bR, Path.Direction.CW)
        pathBody.rewind()
        rect.set(bCx, bCy - bodyHalf, bodyR, bCy + bodyHalf)
        pathBody.addRoundRect(rect, bodyHalf, bodyHalf, Path.Direction.CW)
        pathPod.rewind()
        rect.set(podL, podCy - podHalf, podR, podCy + podHalf)
        pathPod.addRoundRect(rect, podHalf, podHalf, Path.Direction.CW)
        pathNeck.rewind()
        if (neckA > 0.02f) {
            val xa = bodyR - 30f * d
            val xc = podL + 30f * d
            if (xc > xa) {
                val ha = bodyHalf * 0.75f
                val hc = podHalf * 0.75f
                val hn = 19f * d * neckA
                val mx = (xa + xc) / 2f
                val my = (bCy + podCy) / 2f
                pathNeck.moveTo(xa, bCy - ha)
                pathNeck.quadTo(xa + (xc - xa) * 0.25f, my - hn, mx, my - hn)
                pathNeck.quadTo(xa + (xc - xa) * 0.75f, my - hn, xc, podCy - hc)
                pathNeck.lineTo(xc, podCy + hc)
                pathNeck.quadTo(xa + (xc - xa) * 0.75f, my + hn, mx, my + hn)
                pathNeck.quadTo(xa + (xc - xa) * 0.25f, my + hn, xa, bCy + ha)
                pathNeck.close()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (anchor == null && anchorProvider() == null) return
        if (veilA > 0.001f) {
            veilPaint.alpha = (veilA * 255f).toInt()
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), veilPaint)
        }
        buildPaths()
        val blur = 9f * d
        val off = 4f * d
        val hasNeck = !pathNeck.isEmpty
        val dark = pal.shadow
        val light = pal.light
        val peakDark = if (pal.dark) 0.8f else 0.6f
        softShadow(canvas, pathBulb, off, off, dark, blur, peakDark)
        softShadow(canvas, pathBody, off, off, dark, blur, peakDark)
        softShadow(canvas, pathPod, off, off, dark, blur, peakDark)
        if (hasNeck) softShadow(canvas, pathNeck, off, off, dark, blur, peakDark * neckA)
        softShadow(canvas, pathBulb, -off, -off, light, blur, 0.9f)
        softShadow(canvas, pathBody, -off, -off, light, blur, 0.9f)
        softShadow(canvas, pathPod, -off, -off, light, blur, 0.9f)
        if (hasNeck) softShadow(canvas, pathNeck, -off, -off, light, blur, 0.9f * neckA)
        fillPaint.alpha = 255
        canvas.drawPath(pathBulb, fillPaint)
        canvas.drawPath(pathBody, fillPaint)
        canvas.drawPath(pathPod, fillPaint)
        if (hasNeck) {
            fillPaint.alpha = (neckA * 255f).toInt()
            canvas.drawPath(pathNeck, fillPaint)
            fillPaint.alpha = 255
        }
        divPaint.strokeWidth = d
        canvas.drawLine(divX, bCy - divHalf, divX, bCy + divHalf, divPaint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        showing = true
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        anim?.cancel()
        showing = false
        super.onDetachedFromWindow()
    }
}
