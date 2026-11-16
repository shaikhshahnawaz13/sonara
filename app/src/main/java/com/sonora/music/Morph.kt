package com.sonora.music

import android.content.Context
import android.graphics.Outline
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import kotlin.math.abs

/**
 * Content area that turns a clear horizontal swipe into a tab change.
 * Carousels, seek bars and text fields keep their own gestures; vertical scrolling is never taken.
 * One continuous drag can change the tab only once.
 */
class SwipeFrame(ctx: Context, private val enabled: () -> Boolean, private val onSwipe: (Int) -> Unit) : FrameLayout(ctx) {
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private val trigger = ctx.dp(64)
    private var downX = 0f
    private var downY = 0f
    private var tracking = false
    private var fired = false

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                fired = false
                tracking = enabled() && !ownsHorizontal(this, e.x, e.y)
            }
            MotionEvent.ACTION_MOVE -> if (tracking && !fired) {
                val dx = e.x - downX
                val dy = e.y - downY
                if (abs(dx) > maxOf(trigger, slop * 3) && abs(dx) > abs(dy) * 1.8f) {
                    fired = true
                    onSwipe(if (dx < 0) 1 else -1)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> tracking = false
        }
        return false
    }

    override fun onTouchEvent(e: MotionEvent): Boolean = fired

    /** True when the touch lands on something that scrolls or drags horizontally. */
    private fun ownsHorizontal(group: ViewGroup, x: Float, y: Float): Boolean {
        for (i in group.childCount - 1 downTo 0) {
            val ch = group.getChildAt(i)
            if (ch.visibility != VISIBLE) continue
            val cx = x + group.scrollX - ch.left
            val cy = y + group.scrollY - ch.top
            if (cx < 0 || cy < 0 || cx > ch.width || cy > ch.height) continue
            if (ch is HorizontalScrollView || ch is NeoSeekBar || ch is SlimSeek || ch is android.widget.EditText) return true
            if (ch is ViewGroup) return ownsHorizontal(ch, cx, cy)
            return false
        }
        return false
    }
}

/** Rounded-rectangle clip that the player container animates from the mini-player's shape to full screen. */
class ClipShape : ViewOutlineProvider() {
    var l = 0f
    var t = 0f
    var r = 0f
    var b = 0f
    var radius = 0f
    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(l.toInt(), t.toInt(), r.toInt(), b.toInt(), radius)
    }
}

/** Where a shared element starts (mini) and ends (full), expressed as transforms of the full-player view. */
class Shared(val view: View, val fromRect: RectF, val toRect: RectF, private val topLeftPivot: Boolean) {
    private val tx0: Float
    private val ty0: Float
    private val s0: Float

    init {
        if (topLeftPivot) {
            view.pivotX = 0f
            view.pivotY = 0f
            tx0 = fromRect.left - toRect.left
            ty0 = fromRect.top - toRect.top
            s0 = (fromRect.height() / toRect.height()).coerceIn(0.2f, 1f)
        } else {
            view.pivotX = view.width / 2f
            view.pivotY = view.height / 2f
            tx0 = fromRect.centerX() - toRect.centerX()
            ty0 = fromRect.centerY() - toRect.centerY()
            s0 = (fromRect.width() / toRect.width()).coerceIn(0.05f, 1f)
        }
    }

    fun apply(e: Float) {
        val k = 1f - e
        view.translationX = tx0 * k
        view.translationY = ty0 * k
        val s = 1f - (1f - s0) * k
        view.scaleX = s
        view.scaleY = s
    }

    fun reset() {
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
    }
}
