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
