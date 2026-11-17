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
