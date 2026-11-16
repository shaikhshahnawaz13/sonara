package com.sonora.music

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.LruCache
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

// ---------------------------------------------------------------- basics

const val W_REGULAR = 0
const val W_MEDIUM = 1
const val W_BOLD = 2

fun Context.dp(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dp(v: Int): Int = dp(v.toFloat())

fun Context.tv(s: CharSequence, sp: Float = 14f, color: Int = Theme.p.text, weight: Int = W_REGULAR): TextView {
    val t = TextView(this)
    t.text = s
    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    t.setTextColor(color)
    t.includeFontPadding = false
    t.typeface = when (weight) {
        W_BOLD -> Typeface.create("sans-serif", Typeface.BOLD)
        W_MEDIUM -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        else -> Typeface.create("sans-serif", Typeface.NORMAL)
    }
    return t
}

fun TextView.oneLine(): TextView {
    maxLines = 1
    ellipsize = TextUtils.TruncateAt.END
    return this
}

fun Context.iconView(res: Int, tint: Int): ImageView {
    val v = ImageView(this)
    v.setImageResource(res)
    v.setColorFilter(tint)
    v.scaleType = ImageView.ScaleType.FIT_CENTER
    return v
}

fun Context.neo(style: NeoStyle, level: NeoLevel = NeoLevel.STANDARD, radiusDp: Float = 24f, tint: Int = 0): NeoDrawable =
    NeoDrawable(resources.displayMetrics.density, style, level, radiusDp, tint)

/** Lets neo-morphic shadows of children draw past their parent padding instead of being cut into rectangles. */
fun View.unclip() {
    if (this is ViewGroup) {
        clipChildren = false
        clipToPadding = false
        for (i in 0 until childCount) getChildAt(i).unclip()
    }
}

fun View.applyNeo(d: NeoDrawable, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) {
    background = d
    if (this is ViewGroup) { clipChildren = false; clipToPadding = false }
    setPadding(d.margin + l, d.margin + t, d.margin + r, d.margin + b)
}

fun View.neoMargin(): Int = (background as? NeoDrawable)?.margin ?: 0

@SuppressLint("ClickableViewAccessibility")
fun View.pressScale(amount: Float = 0.97f) {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> v.animate().scaleX(amount).scaleY(amount).setDuration(80).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }
        false
    }
}

fun Context.animationsEnabled(): Boolean = try {
    android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
} catch (e: Exception) {
    true
}

fun View.revealIn() {
    if (!context.animationsEnabled()) return
    alpha = 0f
    translationY = context.dp(10f).toFloat()
    animate().alpha(1f).translationY(0f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
}

fun Context.hideKeyboard(view: View) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

/** Adds a neo-morphic child to a vertical list so its visible surface lines up with the screen margin. */
fun LinearLayout.addNeo(child: View, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT, topGap: Int = 0) {
    val m = child.neoMargin()
    val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
    lp.leftMargin = -m
    lp.rightMargin = -m
    lp.topMargin = topGap - m
    lp.bottomMargin = -m
    addView(child, lp)
}

// ---------------------------------------------------------------- components

fun Context.roundButton(
    res: Int,
    desc: String,
    size: Float = 48f,
    level: NeoLevel = NeoLevel.SUBTLE,
    tint: Int = Theme.p.text,
    fill: Int = 0,
    onClick: () -> Unit
): FrameLayout {
    val d = neo(NeoStyle.RAISED, level, -1f, fill)
    val box = FrameLayout(this)
    box.background = d
    val total = dp(size) + 2 * d.margin
    box.layoutParams = LinearLayout.LayoutParams(total, total)
    val iconPx = dp(size * 0.5f)
    box.addView(iconView(res, tint), FrameLayout.LayoutParams(iconPx, iconPx, Gravity.CENTER))
    box.contentDescription = desc
    box.isClickable = true
    box.isFocusable = true
    box.setOnClickListener { onClick() }
    box.pressScale()
    return box
}

fun FrameLayout.buttonIcon(): ImageView = getChildAt(0) as ImageView

fun Context.primaryButton(label: String, iconRes: Int = 0, onClick: () -> Unit): LinearLayout {
    val pal = Theme.p
    val d = neo(NeoStyle.RAISED, NeoLevel.STANDARD, -1f, pal.accent)
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.gravity = Gravity.CENTER
    row.applyNeo(d, dp(24), 0, dp(24), 0)
    row.minimumHeight = dp(54) + 2 * d.margin
    if (iconRes != 0) {
        val lp = LinearLayout.LayoutParams(dp(20), dp(20))
        lp.rightMargin = dp(8)
        row.addView(iconView(iconRes, pal.onAccent), lp)
    }
    row.addView(tv(label, 15f, pal.onAccent, W_BOLD))
    row.isClickable = true
    row.isFocusable = true
    row.contentDescription = label
    row.setOnClickListener { onClick() }
    row.pressScale()
    return row
}

fun Context.chip(label: String, selected: Boolean, onClick: () -> Unit): TextView {
    val pal = Theme.p
    val d = if (selected) neo(NeoStyle.INSET, NeoLevel.SUBTLE, -1f) else neo(NeoStyle.RAISED, NeoLevel.SUBTLE, -1f)
    val t = tv(label, 14f, if (selected) pal.accentText else pal.textSecondary, W_MEDIUM)
    t.gravity = Gravity.CENTER
    t.applyNeo(d, dp(18), dp(10), dp(18), dp(10))
    t.minHeight = dp(44) + 2 * d.margin
    t.isClickable = true
    t.isFocusable = true
    t.setOnClickListener { onClick() }
    return t
}

fun Context.sectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null): LinearLayout {
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.gravity = Gravity.CENTER_VERTICAL
    row.addView(tv(title, 20f, Theme.p.text, W_BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    if (action != null) {
        val a = tv(action, 14f, Theme.p.accentText, W_MEDIUM)
        a.setPadding(dp(8), dp(10), 0, dp(10))
        a.isClickable = true
        a.setOnClickListener { onAction?.invoke() }
        row.addView(a)
    }
    return row
}

fun Context.screenTitle(kicker: String, title: String): LinearLayout {
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    val k = tv(kicker.uppercase(), 12f, Theme.p.accentText, W_BOLD)
    k.letterSpacing = 0.14f
    col.addView(k)
    val t = tv(title, 32f, Theme.p.text, W_BOLD)
    t.setPadding(0, dp(6), 0, 0)
    col.addView(t)
    return col
}

fun Context.logoMark(sizeDp: Float): ImageView {
    val v = ImageView(this)
    v.setImageResource(R.drawable.sonara_mark)
    v.scaleType = ImageView.ScaleType.FIT_CENTER
    v.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    v.contentDescription = "SONARA"
    return v
}

fun Context.infoCard(title: String, body: String, action: String? = null, onAction: (() -> Unit)? = null): LinearLayout {
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.applyNeo(neo(NeoStyle.RAISED, NeoLevel.STANDARD, 24f), dp(20), dp(20), dp(20), dp(20))
    col.addView(logoMark(40f))
    val t = tv(title, 18f, Theme.p.text, W_BOLD)
    t.setPadding(0, dp(14), 0, 0)
    col.addView(t)
    val b = tv(body, 14f, Theme.p.textSecondary)
    b.setLineSpacing(0f, 1.15f)
    b.setPadding(0, dp(6), 0, 0)
    col.addView(b)
    if (action != null && onAction != null) {
        val btn = primaryButton(action, 0, onAction)
        val m = btn.neoMargin()
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(16) - m
        lp.leftMargin = -m
        lp.rightMargin = -m
        lp.bottomMargin = -m
        col.addView(btn, lp)
    }
    return col
}

// ---------------------------------------------------------------- switch

class NeoSwitch(ctx: Context) : View(ctx) {
    var onChange: ((Boolean) -> Unit)? = null
    private var pos = 0f
    private var animator: ValueAnimator? = null
    private val trackD = context.neo(NeoStyle.INSET, NeoLevel.SUBTLE, -1f)
    private val knobOff = context.neo(NeoStyle.RAISED, NeoLevel.SUBTLE, -1f)
    private val knobOn = context.neo(NeoStyle.RAISED, NeoLevel.SUBTLE, -1f, Theme.p.accent)
    val m = knobOff.margin
    private val surfaceW = context.dp(56)
    private val surfaceH = context.dp(28)
    private val knobS = context.dp(22)

    var checked = false
        private set

    init {
        isClickable = true
        isFocusable = true
        setOnClickListener { setChecked(!checked, true) }
    }

    fun setChecked(v: Boolean, notify: Boolean) {
        if (v == checked) return
        checked = v
        animator?.cancel()
        val target = if (v) 1f else 0f
        if (context.animationsEnabled() && isAttachedToWindow) {
            val a = ValueAnimator.ofFloat(pos, target)
            a.duration = 180
            a.addUpdateListener { pos = it.animatedValue as Float; invalidate() }
            animator = a
            a.start()
        } else {
            pos = target
            invalidate()
        }
        if (notify) onChange?.invoke(v)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(surfaceW + 2 * m, surfaceH + 2 * m)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        trackD.setBounds(m, m, width - m, height - m)
        trackD.draw(canvas)
        val travel = (surfaceW - surfaceH).toFloat()
        val cx = m + surfaceH / 2f + travel * pos
        val cy = height / 2f
        val side = knobS + 2 * m
        val d = if (checked) knobOn else knobOff
        d.setBounds((cx - side / 2f).toInt(), (cy - side / 2f).toInt(), (cx + side / 2f).toInt(), (cy + side / 2f).toInt())
        d.draw(canvas)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Switch"
        info.isCheckable = true
        info.isChecked = checked
    }
}

// ---------------------------------------------------------------- seek bar

class NeoSeekBar(ctx: Context) : View(ctx) {
    var max = 1000
    var onScrub: ((Int) -> Unit)? = null
    var onCommit: ((Int) -> Unit)? = null

    private var progressValue = 0
    private var dragging = false
    private var dragValue = 0
    private val trackD = context.neo(NeoStyle.INSET, NeoLevel.SUBTLE, -1f)
    private val thumbD = context.neo(NeoStyle.RAISED, NeoLevel.SUBTLE, -1f)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val thumbSurface = context.dp(22)
    private val pad = thumbD.margin + thumbSurface / 2

    init {
        fill.color = Theme.p.accent
        dot.color = Theme.p.accent
        contentDescription = "Seek"
    }

    fun setProgress(v: Int) {
        if (dragging) return
        progressValue = v.coerceIn(0, maxOf(1, max))
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(context.dp(200), widthMeasureSpec)
        setMeasuredDimension(w, thumbSurface + 2 * thumbD.margin + context.dp(8))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = pad.toFloat()
        val right = (width - pad).toFloat()
        val cy = height / 2f
        val trackH = context.dp(10f).toFloat()
        trackD.setBounds(left.toInt(), (cy - trackH / 2f).toInt(), right.toInt(), (cy + trackH / 2f).toInt())
        trackD.draw(canvas)
        val shown = if (dragging) dragValue else progressValue
        val frac = if (max > 0) shown.toFloat() / max else 0f
        val x = left + (right - left) * frac
        val inset = context.dp(2f).toFloat()
        if (x - left > inset * 2) {
            rect.set(left + inset, cy - trackH / 2f + inset, x, cy + trackH / 2f - inset)
            canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, fill)
        }
        val side = thumbSurface + 2 * thumbD.margin
        thumbD.setBounds((x - side / 2f).toInt(), (cy - side / 2f).toInt(), (x + side / 2f).toInt(), (cy + side / 2f).toInt())
        thumbD.draw(canvas)
        canvas.drawCircle(x, cy, context.dp(4f).toFloat(), dot)
    }

    private fun valueAt(x: Float): Int {
        val left = pad.toFloat()
        val right = (width - pad).toFloat()
        val f = ((x - left) / (right - left)).coerceIn(0f, 1f)
        return (f * max).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                dragging = true
                dragValue = valueAt(e.x)
                onScrub?.invoke(dragValue)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                dragValue = valueAt(e.x)
                onScrub?.invoke(dragValue)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                dragValue = valueAt(e.x)
                dragging = false
                progressValue = dragValue
                onCommit?.invoke(dragValue)
                invalidate()
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                invalidate()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}

// ---------------------------------------------------------------- artwork

class ArtworkView(ctx: Context, radiusDp: Float) : ImageView(ctx) {
    private val radiusPx = radiusDp * ctx.resources.displayMetrics.density
    private var placeholder = true

    init {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
            }
        }
        clipToOutline = true
        showPlaceholder()
    }

    fun showPlaceholder() {
        placeholder = true
        setBackgroundColor(Theme.blend(Theme.p.bg, Theme.p.shadow, 0.18f))
        scaleType = ScaleType.FIT_CENTER
        setImageResource(R.drawable.ic_music_note)
        setColorFilter(Theme.p.textMuted)
        updatePadding()
    }

    fun showBitmap(b: Bitmap) {
        placeholder = false
        clearColorFilter()
        scaleType = ScaleType.CENTER_CROP
        setPadding(0, 0, 0, 0)
        setImageBitmap(b)
    }

    private fun updatePadding() {
        if (!placeholder) return
        val p = minOf(width, height) / 4
        setPadding(p, p, p, p)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePadding()
    }
}

object Artwork {
    private val cache = object : LruCache<String, Bitmap>(maxOf(4 * 1024 * 1024, (Runtime.getRuntime().maxMemory() / 16).toInt())) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val missing = HashSet<String>()
    private val pool = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())

    private fun key(t: Track, size: Int): String = (if (t.albumId > 0) "a${t.albumId}" else "t${t.id}") + ":" + size

    fun bind(view: ArtworkView, t: Track?, sizePx: Int) {
        if (t == null) {
            view.tag = null
            view.showPlaceholder()
            return
        }
        val k = key(t, sizePx)
        view.tag = k
        val hit = cache.get(k)
        if (hit != null) {
            view.showBitmap(hit)
            return
        }
        view.showPlaceholder()
        if (missing.contains(k)) return
        val app = view.context.applicationContext
        pool.execute {
            val bmp = decode(app, t, sizePx)
            main.post {
                if (bmp != null) cache.put(k, bmp) else missing.add(k)
                if (bmp != null && view.tag == k) view.showBitmap(bmp)
            }
        }
    }

    /** For background threads (notification and lock-screen artwork). */
    fun decodeSync(ctx: Context, t: Track, sizePx: Int): Bitmap? {
        val k = key(t, sizePx)
        return cache.get(k) ?: decode(ctx.applicationContext, t, sizePx)
    }

    private fun decode(ctx: Context, t: Track, size: Int): Bitmap? {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                return ctx.contentResolver.loadThumbnail(t.uri, Size(size, size), null)
            }
            if (t.albumId <= 0) return null
            val uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), t.albumId)
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= size && bounds.outHeight / (sample * 2) >= size) sample *= 2
            val opts = BitmapFactory.Options()
            opts.inSampleSize = sample
            return ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
            return null
        }
    }
}

// ---------------------------------------------------------------- track row

class TrackRowView(ctx: Context) : LinearLayout(ctx) {
    private val artPx = context.dp(48)
    private val art = ArtworkView(ctx, 12f)
    private val title = context.tv("", 15f, Theme.p.text, W_MEDIUM).oneLine()
    private val subtitle = context.tv("", 13f, Theme.p.textSecondary).oneLine()
    private val duration = context.tv("", 12f, Theme.p.textMuted)
    private val more = ImageView(ctx)
    private val idleBg = StateListDrawable()
    private val playingBg = context.neo(NeoStyle.INSET, NeoLevel.SUBTLE, 16f)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false
        setPadding(context.dp(8), context.dp(8), context.dp(2), context.dp(8))
        minimumHeight = context.dp(64)
        isClickable = true
        isFocusable = true
        idleBg.addState(intArrayOf(android.R.attr.state_pressed), context.neo(NeoStyle.INSET, NeoLevel.SUBTLE, 16f))
        idleBg.addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        addView(art, LayoutParams(artPx, artPx))
        val texts = LinearLayout(ctx)
        texts.orientation = VERTICAL
        texts.addView(title)
        subtitle.setPadding(0, context.dp(3), 0, 0)
        texts.addView(subtitle)
        val tlp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        tlp.leftMargin = context.dp(12)
        addView(texts, tlp)
        val dlp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        dlp.leftMargin = context.dp(8)
        addView(duration, dlp)
        more.setImageResource(R.drawable.ic_more)
        more.setColorFilter(Theme.p.textSecondary)
        more.setPadding(context.dp(10), context.dp(10), context.dp(10), context.dp(10))
        more.contentDescription = "More options"
        more.isClickable = true
        addView(more, LayoutParams(context.dp(44), context.dp(44)))
    }

    fun bind(t: Track, playing: Boolean, onClick: () -> Unit, onMore: (View) -> Unit) {
        title.text = t.title
        title.setTextColor(if (playing) Theme.p.accentText else Theme.p.text)
        subtitle.text = t.artist
        duration.text = formatTime(t.durationMs.toInt())
        Artwork.bind(art, t, artPx)
        background = if (playing) playingBg else idleBg
        setOnClickListener { onClick() }
        more.setOnClickListener { onMore(it) }
    }
}

// ---------------------------------------------------------------- layout helper

/** Centres its single child as the largest square that fits. */
class FitSquare(ctx: Context) : FrameLayout(ctx) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val c = getChildAt(0) ?: return
        val w = r - l
        val h = b - t
        val s = minOf(w, h)
        c.measure(MeasureSpec.makeMeasureSpec(s, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(s, MeasureSpec.EXACTLY))
        val left = (w - s) / 2
        val top = (h - s) / 2
        c.layout(left, top, left + s, top + s)
    }
}
