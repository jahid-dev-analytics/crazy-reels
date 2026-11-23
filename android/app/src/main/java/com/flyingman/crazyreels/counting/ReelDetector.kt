package com.flyingman.crazyreels.counting

import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Decides, per app, when the user has moved on to a new reel.
 * Implementations are written from layouts recorded on a real phone.
 */
interface ReelDetector {
    /**
     * Returns true exactly once for each new reel that comes on screen.
     * [root] fetches the current window's layout, which is slow, so call it only when needed.
     */
    fun onEvent(event: AccessibilityEvent, root: () -> AccessibilityNodeInfo?): Boolean
}

/**
 * For apps that show reels in a full-screen pager whose scroll events carry
 * the page index. Counts the first page when the pager appears, then every
 * page further down than any seen so far, so swiping back up and the double
 * event each swipe sends are not counted twice.
 */
class PagerReelDetector(
    private val pagerViewId: String,
    private val pagerClassName: String,
) : ReelDetector {
    private var pager: AccessibilityNodeInfo? = null
    private var furthest = -1

    override fun onEvent(event: AccessibilityEvent, root: () -> AccessibilityNodeInfo?): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return false
        val index = event.fromIndex
        if (index < 0) return false

        val source = event.source
        val sourceId = source?.viewIdResourceName
        when {
            sourceId == pagerViewId -> Unit
            // Some scroll events arrive without their view id (seen on Instagram 452).
            // Accept them only if the class matches and the reels pager is on screen.
            sourceId == null && event.className == pagerClassName && pagerOnScreen(root()) -> Unit
            else -> return false
        }

        // A different pager, or the same one refreshed back to the top, starts a new run.
        val newPager = sourceId != null && source != pager
        if (newPager || (index == 0 && furthest > 0)) {
            if (newPager) pager = source
            furthest = -1
        }
        if (index <= furthest) return false
        furthest = index
        return true
    }

    private fun pagerOnScreen(root: AccessibilityNodeInfo?): Boolean {
        val node = root?.findAccessibilityNodeInfosByViewId(pagerViewId)?.firstOrNull() ?: return false
        val bounds = Rect().also(node::getBoundsInScreen)
        // When the user switches tab, the pager slides off to the right (left edge = screen width).
        return node.isVisibleToUser && bounds.left == 0 && bounds.width() > 0
    }
}

object ReelDetectors {
    private val detectors: Map<ReelApp, ReelDetector> = mapOf(
        // Recorded on Instagram 452 (Oct 2026): Reels tab, and reels opened from feed or profile.
        ReelApp.INSTAGRAM to PagerReelDetector(
            pagerViewId = "com.instagram.android:id/clips_viewer_view_pager",
            pagerClassName = "androidx.viewpager.widget.ViewPager",
        ),
    )

    fun forApp(app: ReelApp): ReelDetector? = detectors[app]
}
