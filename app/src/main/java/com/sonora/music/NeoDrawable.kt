package com.sonora.music

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.LruCache

enum class NeoStyle { RAISED, INSET }

/** The only three elevation levels used anywhere in the app. */
enum class NeoLevel(val depthDp: Float, val blurDp: Float, val marginDp: Float) {
    SUBTLE(3f, 5f, 12f),
    STANDARD(4f, 8f, 17f),
    PROMINENT(5f, 10f, 21f)
}

private object NeoCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, value: Bitmap) { cache.put(key, value) }
}

/**
 * A neo-morphic surface. Raised surfaces cast a dark shadow bottom-right and a light shadow top-left;
 * inset surfaces do the opposite on the inside. The result is rendered once into a bitmap and cached,
 * so scrolling and drawing never recompute blurs. A raised surface that is pressed redraws as inset.
 * The shadow lives inside the drawable bounds, [margin] is the space reserved for it.
 */
class NeoDrawable(
    private val density: Float,
    private val style: NeoStyle,
    private val level: NeoLevel = NeoLevel.STANDARD,
    private val radiusDp: Float = 24f,
    private val tint: Int = 0,
    marginDp: Float = -1f
) : Drawable() {

    val margin: Int = if (marginDp >= 0f) (marginDp * density).toInt()
    else if (style == NeoStyle.RAISED) (level.marginDp * density).toInt() else 0

    private var pressedNow = false
    private var lastW = -1
    private var lastH = -1
    private var lastMode = -1
    private var bitmap: Bitmap? = null
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    override fun isStateful(): Boolean = style == NeoStyle.RAISED

    override fun onStateChange(state: IntArray): Boolean {
        var pressed = false
        for (s in state) if (s == android.R.attr.state_pressed) pressed = true
        if (pressed == pressedNow) return false
        pressedNow = pressed
        rebuild()
        invalidateSelf()
        return true
    }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        rebuild()
    }

    private fun rebuild() {
        val w = bounds.width()
        val h = bounds.height()
        if (w <= 0 || h <= 0) { bitmap = null; return }
        val mode = if (style == NeoStyle.INSET) MODE_INSET else if (pressedNow) MODE_PRESSED else MODE_RAISED
        if (w == lastW && h == lastH && mode == lastMode && bitmap != null) return
        lastW = w
        lastH = h
        lastMode = mode
        val key = "${if (Theme.p.dark) "d" else "l"}|$mode|$level|$radiusDp|$tint|$margin|$w|$h"
        var bmp = NeoCache.get(key)
        if (bmp == null) {
            bmp = render(w, h, mode)
            NeoCache.put(key, bmp)
        }
        bitmap = bmp
    }

    private fun render(w: Int, h: Int, mode: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val m = margin.toFloat()
        val rect = RectF(m, m, w - m, h - m)
        if (rect.width() < 2f || rect.height() < 2f) return bmp
        val maxR = minOf(rect.width(), rect.height()) / 2f
        val r = if (radiusDp < 0f) maxR else minOf(radiusDp * density, maxR)
        val pal = Theme.p
        val depth = level.depthDp * density
        val blur = level.blurDp * density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (mode == MODE_RAISED) {
            paint.color = pal.bg
            paint.setShadowLayer(blur, depth, depth, pal.shadow)
            canvas.drawRoundRect(rect, r, r, paint)
            paint.setShadowLayer(blur, -depth, -depth, pal.light)
            canvas.drawRoundRect(rect, r, r, paint)
            paint.clearShadowLayer()
            val top = if (tint != 0) Theme.blend(tint, Color.WHITE, 0.14f) else pal.surfaceTop
            val bottom = if (tint != 0) tint else pal.surfaceBottom
            paint.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, top, bottom, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(rect, r, r, paint)
            paint.shader = null
        } else {
            val base = if (mode == MODE_PRESSED && tint != 0) Theme.blend(tint, Color.BLACK, 0.18f)
            else Theme.blend(pal.bg, pal.shadow, 0.10f)
            paint.color = base
            canvas.drawRoundRect(rect, r, r, paint)

            val layer = canvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), null)
            val frame = Path()
            frame.fillType = Path.FillType.EVEN_ODD
            val big = 40f * density
            frame.addRect(rect.left - big, rect.top - big, rect.right + big, rect.bottom + big, Path.Direction.CW)
            frame.addRoundRect(rect, r, r, Path.Direction.CW)
            val d = if (mode == MODE_PRESSED) depth * 0.7f else depth
            paint.color = pal.bg
            paint.setShadowLayer(blur, d, d, pal.shadow)
            canvas.drawPath(frame, paint)
            paint.setShadowLayer(blur, -d, -d, pal.light)
            canvas.drawPath(frame, paint)
            paint.clearShadowLayer()
            val mask = Paint(Paint.ANTI_ALIAS_FLAG)
            mask.color = Color.BLACK
            mask.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            canvas.drawRoundRect(rect, r, r, mask)
            canvas.restoreToCount(layer)
        }
        return bmp
    }

    override fun draw(canvas: Canvas) {
        val b = bitmap ?: return
        canvas.drawBitmap(b, bounds.left.toFloat(), bounds.top.toFloat(), paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private const val MODE_RAISED = 0
        private const val MODE_INSET = 1
        private const val MODE_PRESSED = 2
    }
}
