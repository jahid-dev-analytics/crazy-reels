package com.flyingman.crazyreels.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.flyingman.crazyreels.core.BrainState

private val Pink = Color(0xFFC2366B)
private val PinkDark = Color(0xFFEC6E9B)

private val LightColors = lightColorScheme(
    primary = Pink,
    onPrimary = Color.White,
    background = Color(0xFFF3F6F5),
    onBackground = Color(0xFF15201D),
    surface = Color.White,
    onSurface = Color(0xFF15201D),
    onSurfaceVariant = Color(0xFF5B6A66),
)

private val DarkColors = darkColorScheme(
    primary = PinkDark,
    onPrimary = Color(0xFF2A0A17),
    background = Color(0xFF101614),
    onBackground = Color(0xFFE6EEEB),
    surface = Color(0xFF18201D),
    onSurface = Color(0xFFE6EEEB),
    onSurfaceVariant = Color(0xFF9AABA6),
)

/** Skin colour of the brain for each state, same scale as the plan page. */
fun BrainState.color(dark: Boolean): Color = when (this) {
    BrainState.FRESH -> if (dark) Color(0xFF43C28A) else Color(0xFF2F9E6E)
    BrainState.TIRED -> if (dark) Color(0xFFA5CC4F) else Color(0xFF8BB339)
    BrainState.FOGGY -> if (dark) Color(0xFFF0BB45) else Color(0xFFE0A526)
    BrainState.HEAVY -> if (dark) Color(0xFFF08550) else Color(0xFFE06B2B)
    BrainState.ROTTEN -> if (dark) Color(0xFFC47FA2) else Color(0xFF7A3F5E)
}

@Composable
fun CrazyReelsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
