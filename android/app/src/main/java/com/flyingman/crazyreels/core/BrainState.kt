package com.flyingman.crazyreels.core

import androidx.annotation.StringRes
import com.flyingman.crazyreels.R

/**
 * How the brain looks for a given number of reels today.
 * [minReels] is the first count at which the state starts.
 */
enum class BrainState(val minReels: Int, @StringRes val label: Int, @StringRes val line: Int) {
    FRESH(0, R.string.brain_fresh, R.string.brain_fresh_line),
    TIRED(50, R.string.brain_tired, R.string.brain_tired_line),
    FOGGY(100, R.string.brain_foggy, R.string.brain_foggy_line),
    HEAVY(150, R.string.brain_heavy, R.string.brain_heavy_line),
    ROTTEN(200, R.string.brain_rotten, R.string.brain_rotten_line);

    /** 0f at the start of this state, 1f at the start of the next one. */
    fun progress(reels: Int): Float {
        val next = entries.getOrNull(ordinal + 1) ?: return 1f
        return ((reels - minReels).toFloat() / (next.minReels - minReels)).coerceIn(0f, 1f)
    }

    companion object {
        fun forReels(reels: Int): BrainState = entries.last { reels >= it.minReels }
    }
}
