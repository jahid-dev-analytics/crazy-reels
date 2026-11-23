package com.flyingman.crazyreels.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User choices that both the app screen and the counting service read. */
object AppSettings {
    private lateinit var prefs: SharedPreferences

    private val _liveCounter = MutableStateFlow(true)
    /** Show the floating reel counter on top of the supported apps. */
    val liveCounter: StateFlow<Boolean> = _liveCounter.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        _liveCounter.value = prefs.getBoolean("live_counter", true)
    }

    fun setLiveCounter(on: Boolean) {
        prefs.edit().putBoolean("live_counter", on).apply()
        _liveCounter.value = on
    }

    /** Where the user last dragged the floating counter, or null for the default spot. */
    var bubblePosition: Pair<Int, Int>?
        get() = if (prefs.contains("bubble_x")) prefs.getInt("bubble_x", 0) to prefs.getInt("bubble_y", 0) else null
        set(value) {
            prefs.edit().apply {
                if (value == null) remove("bubble_x").remove("bubble_y")
                else putInt("bubble_x", value.first).putInt("bubble_y", value.second)
            }.apply()
        }
}
