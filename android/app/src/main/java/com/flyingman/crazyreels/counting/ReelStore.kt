package com.flyingman.crazyreels.counting

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class DayCounts(val date: LocalDate, val perApp: Map<ReelApp, Int>) {
    val total: Int get() = perApp.values.sum()
}

/**
 * Today's reel counts per app. Kept in SharedPreferences for phase 1;
 * phase 2 moves history into a Room database.
 */
object ReelStore {
    private lateinit var prefs: SharedPreferences
    private val _today = MutableStateFlow(DayCounts(LocalDate.now(), emptyMap()))
    val today: StateFlow<DayCounts> = _today.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("reel_counts", Context.MODE_PRIVATE)
        _today.value = load(LocalDate.now())
    }

    @Synchronized
    fun add(app: ReelApp) {
        val date = LocalDate.now()
        val current = if (_today.value.date == date) _today.value else load(date)
        val next = (current.perApp[app] ?: 0) + 1
        prefs.edit().putInt(key(date, app), next).apply()
        _today.value = current.copy(perApp = current.perApp + (app to next))
    }

    /** Re-reads today, so a screen left open past midnight shows the new day. */
    fun refresh() {
        _today.value = load(LocalDate.now())
    }

    private fun load(date: LocalDate) = DayCounts(
        date,
        ReelApp.entries.associateWith { prefs.getInt(key(date, it), 0) }.filterValues { it > 0 },
    )

    private fun key(date: LocalDate, app: ReelApp) = "$date|${app.name}"
}
