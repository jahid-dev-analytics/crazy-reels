package com.flyingman.crazyreels.debug

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Developer tool: while switched on, writes scroll events and throttled
 * screen-layout snapshots of the supported apps to JSON-lines files in the
 * app's private storage. We read them over adb to write each app's detector,
 * and clear them over adb too (run-as ... rm), so nothing on screen deletes them.
 * Nothing is uploaded.
 */
object LayoutRecorder {
    private const val SNAPSHOT_GAP_MS = 1200L
    private const val MAX_NODES = 600
    private const val MAX_TEXT = 80

    private lateinit var prefs: SharedPreferences
    private lateinit var dir: File
    private var lastSnapshotAt = 0L

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _lines = MutableStateFlow(0)
    /** Lines currently saved across all recording files. */
    val lines: StateFlow<Int> = _lines.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("dev", Context.MODE_PRIVATE)
        dir = File(context.applicationContext.filesDir, "recordings").apply { mkdirs() }
        _enabled.value = prefs.getBoolean("recorder", false)
        _lines.value = dir.listFiles().orEmpty().sumOf { f -> f.useLines { it.count() } }
    }

    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean("recorder", on).apply()
        _enabled.value = on
    }

    fun onEvent(event: AccessibilityEvent, root: () -> AccessibilityNodeInfo?) {
        if (!_enabled.value) return
        val pkg = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            write(pkg, eventJson(event, now))
        }
        if (now - lastSnapshotAt >= SNAPSHOT_GAP_MS) {
            val node = root() ?: return
            if (node.packageName?.toString() != pkg) return
            lastSnapshotAt = now
            write(pkg, snapshotJson(node, pkg, now))
        }
    }

    private fun eventJson(e: AccessibilityEvent, now: Long) = JSONObject().apply {
        put("t", now)
        // When Android recorded the event (uptime ms); "t" is when we handled it.
        put("et", e.eventTime)
        put("k", "event")
        put("type", AccessibilityEvent.eventTypeToString(e.eventType))
        put("cls", e.className)
        put("src", e.source?.viewIdResourceName)
        put("from", e.fromIndex)
        put("to", e.toIndex)
        put("count", e.itemCount)
        put("scrollY", e.scrollY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) put("dy", e.scrollDeltaY)
        e.text.firstOrNull()?.let { put("text", it.toString().take(MAX_TEXT)) }
    }

    private fun snapshotJson(root: AccessibilityNodeInfo, pkg: String, now: Long): JSONObject {
        val nodes = JSONArray()
        val rect = Rect()
        // Depth-first walk, flattened with a depth field to keep lines compact.
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>().apply { add(root to 0) }
        while (stack.isNotEmpty() && nodes.length() < MAX_NODES) {
            val (n, depth) = stack.removeLast()
            n.getBoundsInScreen(rect)
            nodes.put(JSONObject().apply {
                put("d", depth)
                put("cls", n.className?.toString()?.substringAfterLast('.'))
                n.viewIdResourceName?.let { put("id", it.substringAfter(":id/")) }
                n.contentDescription?.let { put("desc", it.toString().take(MAX_TEXT)) }
                n.text?.let { put("txt", it.toString().take(MAX_TEXT)) }
                put("b", "${rect.left},${rect.top},${rect.right},${rect.bottom}")
                if (n.isScrollable) put("scroll", true)
                if (!n.isVisibleToUser) put("hidden", true)
                if (n.isSelected) put("sel", true)
            })
            for (i in n.childCount - 1 downTo 0) n.getChild(i)?.let { stack.add(it to depth + 1) }
        }
        return JSONObject().apply {
            put("t", now)
            put("k", "snapshot")
            put("pkg", pkg)
            put("nodes", nodes)
        }
    }

    private fun write(pkg: String, json: JSONObject) {
        File(dir, "$pkg.jsonl").appendText(json.toString() + "\n")
        _lines.value += 1
    }
}
