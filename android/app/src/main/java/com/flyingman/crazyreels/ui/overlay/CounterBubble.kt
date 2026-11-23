package com.flyingman.crazyreels.ui.overlay

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.flyingman.crazyreels.core.AppSettings
import com.flyingman.crazyreels.core.BrainState
import com.flyingman.crazyreels.ui.theme.color
import kotlin.math.abs

/**
 * The floating pill that shows today's reel count on top of the supported apps.
 * Uses an accessibility overlay window, so it needs no extra permission.
 */
class CounterBubble(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val density = service.resources.displayMetrics.density
    private var view: TextView? = null
    private var count = 0

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        val saved = AppSettings.bubblePosition
        x = saved?.first ?: dp(12)
        y = saved?.second ?: dp(160)
    }

    val isShowing: Boolean get() = view != null

    fun show() {
        if (view != null) return
        view = createView().also {
            render(it)
            windowManager.addView(it, params)
        }
    }

    fun hide() {
        view?.let(windowManager::removeView)
        view = null
    }

    fun setCount(newCount: Int) {
        val grew = newCount > count
        count = newCount
        view?.let {
            render(it)
            if (grew) pop(it)
        }
    }

    private fun render(v: TextView) {
        v.text = count.toString()
        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(BrainState.forReels(count).color(dark = true).toArgb())
            setSize(dp(12), dp(12))
        }
        v.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
    }

    private fun pop(v: TextView) {
        v.animate().cancel()
        v.scaleX = 1.25f
        v.scaleY = 1.25f
        v.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createView() = TextView(service).apply {
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        typeface = Typeface.DEFAULT_BOLD
        compoundDrawablePadding = dp(8)
        setPadding(dp(14), dp(8), dp(16), dp(8))
        background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(Color.argb(210, 21, 32, 29))
            setStroke(dp(1), Color.argb(90, 255, 255, 255))
        }
        elevation = dp(4).toFloat()
        contentDescription = "Crazy Reels counter"

        // Drag to move; the spot is remembered for next time.
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; startX = params.x; startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (e.rawX - downX).toInt()
                    params.y = startY + (e.rawY - downY).toInt()
                    windowManager.updateViewLayout(v, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (abs(e.rawX - downX) > dp(4) || abs(e.rawY - downY) > dp(4)) {
                        AppSettings.bubblePosition = params.x to params.y
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(value: Int) = (value * density).toInt()
}
