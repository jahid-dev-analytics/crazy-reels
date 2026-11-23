package com.flyingman.crazyreels.counting

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.flyingman.crazyreels.core.AppSettings
import com.flyingman.crazyreels.debug.LayoutRecorder
import com.flyingman.crazyreels.ui.overlay.CounterBubble
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Reads the screen of the supported apps only (see res/xml/reel_counter_service.xml),
 * adds one to today's count each time a new reel comes on screen, and shows the
 * floating live counter while one of those apps is open.
 */
class ReelCounterService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = MainScope()
    private var bubble: CounterBubble? = null

    /** The reel on screen now, waiting to be counted once it has been watched long enough. */
    private var shown: Shown? = null

    private class Shown(val app: ReelApp, val index: Int, val since: Long, val count: Runnable)

    override fun onCreate() {
        super.onCreate()
        ReelStore.init(this)
        LayoutRecorder.init(this)
        AppSettings.init(this)
    }

    override fun onServiceConnected() {
        val bubble = CounterBubble(this).also { bubble = it }
        scope.launch { ReelStore.today.collect { bubble.setCount(it.total) } }
        scope.launch { AppSettings.liveCounter.collect { on -> if (!on) bubble.hide() } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val app = ReelApp.fromPackage(event.packageName) ?: return
        showBubble()
        LayoutRecorder.onEvent(event) { rootInActiveWindow }
        if (ReelDetectors.forApp(app)?.onEvent(event) { rootInActiveWindow } == true) {
            onNewReel(app, event.fromIndex, event.eventTime)
        }
    }

    /**
     * A new reel came on screen at [eventTime] (uptime ms). The previous one counts only if it
     * stayed on screen for [MIN_VIEW_MS], so flicking past reels doesn't add to the total.
     * Timing uses the event's own timestamp, so events that arrive late in a burst are still judged
     * by when they really happened.
     */
    private fun onNewReel(app: ReelApp, index: Int, eventTime: Long) {
        shown?.let { previous ->
            handler.removeCallbacks(previous.count)
            if (eventTime - previous.since >= MIN_VIEW_MS) count(previous, "next reel after ${eventTime - previous.since}ms")
            else Log.i(TAG, "skipped: ${previous.app.name} index=${previous.index} on screen ${eventTime - previous.since}ms")
        }
        lateinit var next: Shown
        next = Shown(app, index, eventTime, Runnable { count(next, "watched"); shown = null })
        shown = next
        val waited = SystemClock.uptimeMillis() - eventTime
        handler.postDelayed(next.count, (MIN_VIEW_MS - waited).coerceAtLeast(0))
    }

    private fun count(reel: Shown, why: String) {
        ReelStore.add(reel.app)
        Log.i(TAG, "reel counted: ${reel.app.name} index=${reel.index} ($why) total=${ReelStore.today.value.total}")
    }

    /**
     * We only get events from the supported apps, so check every second whether one is still in
     * front. Other apps' windows come back as null here, so two misses in a row hide the bubble;
     * one miss is tolerated because the root is briefly null during screen changes.
     */
    private val checkForeground = object : Runnable {
        private var misses = 0

        override fun run() {
            misses = if (ReelApp.fromPackage(rootInActiveWindow?.packageName) != null) 0 else misses + 1
            if (misses >= 2) {
                misses = 0
                bubble?.hide()
            } else {
                handler.postDelayed(this, FOREGROUND_CHECK_MS)
            }
        }
    }

    private fun showBubble() {
        val bubble = bubble ?: return
        if (bubble.isShowing || !AppSettings.liveCounter.value) return
        bubble.show()
        handler.removeCallbacks(checkForeground)
        handler.postDelayed(checkForeground, FOREGROUND_CHECK_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        bubble?.hide()
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val TAG = "CrazyReels"

        /** How long a reel must stay on screen to count as watched. */
        private const val MIN_VIEW_MS = 800L

        private const val FOREGROUND_CHECK_MS = 1000L

        fun isEnabled(context: Context): Boolean {
            val me = ComponentName(context, ReelCounterService::class.java)
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
