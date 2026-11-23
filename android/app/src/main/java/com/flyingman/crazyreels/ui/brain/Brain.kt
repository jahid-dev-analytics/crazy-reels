package com.flyingman.crazyreels.ui.brain

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.flyingman.crazyreels.core.BrainState
import com.flyingman.crazyreels.ui.theme.color

/**
 * The Crazy Reels brain: a lumpy blob whose colour, eyes and mouth
 * get worse as [reels] climbs. Drawn in code so it scales to any size.
 */
@Composable
fun Brain(reels: Int, modifier: Modifier = Modifier) {
    val state = BrainState.forReels(reels)
    val skin by animateColorAsState(state.color(isSystemInDarkTheme()), label = "skin")
    // 0 = happy and awake, 1 = fully rotten. Moves smoothly across all five states.
    val rot by animateFloatAsState(
        ((state.ordinal + state.progress(reels)) / BrainState.entries.size).coerceIn(0f, 1f),
        label = "rot",
    )

    Canvas(modifier) {
        val unit = size.minDimension / 10f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawLobes(center, unit, skin, rot)
        drawFolds(center, unit, rot)
        drawFace(center, unit, rot, state == BrainState.ROTTEN)
        if (state == BrainState.ROTTEN) drawStinkLines(center, unit)
    }
}

private val Ink = Color(0xFF15201D)

private fun DrawScope.drawLobes(c: Offset, u: Float, skin: Color, rot: Float) {
    // Rotting brains sag a little.
    val sag = rot * 0.5f * u
    val lobes = listOf(
        Offset(-2.2f, -1.2f) to 2.3f, Offset(0f, -2.0f) to 2.5f, Offset(2.2f, -1.2f) to 2.3f,
        Offset(-2.6f, 1.0f) to 2.1f, Offset(0f, 0.8f) to 2.8f, Offset(2.6f, 1.0f) to 2.1f,
    )
    lobes.forEach { (o, r) -> drawCircle(skin, r * u, Offset(c.x + o.x * u, c.y + o.y * u + sag)) }
}

private fun DrawScope.drawFolds(c: Offset, u: Float, rot: Float) {
    val fold = Color.Black.copy(alpha = 0.18f + rot * 0.12f)
    val stroke = Stroke(width = u * 0.22f, cap = StrokeCap.Round)
    val arcs = listOf(Offset(-3.4f, -3.0f), Offset(-0.9f, -3.9f), Offset(1.6f, -3.0f))
    arcs.forEach { o ->
        drawArc(fold, 200f, 140f, false, Offset(c.x + o.x * u, c.y + o.y * u), Size(1.8f * u, 1.6f * u), style = stroke)
    }
    // Centre split between the two halves.
    drawLine(fold, Offset(c.x, c.y - 4.2f * u), Offset(c.x, c.y - 2.6f * u), u * 0.22f, StrokeCap.Round)
}

private fun DrawScope.drawFace(c: Offset, u: Float, rot: Float, dead: Boolean) {
    val eyeY = c.y + 0.1f * u
    val stroke = u * 0.28f
    listOf(-1.3f, 1.3f).forEach { dx ->
        val e = Offset(c.x + dx * u, eyeY)
        if (dead) {
            val d = 0.45f * u
            drawLine(Ink, Offset(e.x - d, e.y - d), Offset(e.x + d, e.y + d), stroke, StrokeCap.Round)
            drawLine(Ink, Offset(e.x - d, e.y + d), Offset(e.x + d, e.y - d), stroke, StrokeCap.Round)
        } else {
            // Eyes close from round to a flat line as the brain gets tired.
            val open = (1f - rot * 1.25f).coerceIn(0.12f, 1f)
            drawOval(Ink, Offset(e.x - 0.42f * u, e.y - 0.5f * u * open), Size(0.84f * u, u * open))
        }
    }
    // Mouth: smile at rot 0, flat in the middle, frown near the end.
    val curve = (0.5f - rot) * 2f * u
    val mouth = Path().apply {
        moveTo(c.x - 1.3f * u, c.y + 1.5f * u)
        quadraticTo(c.x, c.y + 1.5f * u + curve, c.x + 1.3f * u, c.y + 1.5f * u)
    }
    drawPath(mouth, Ink, style = Stroke(width = stroke, cap = StrokeCap.Round))
}

private fun DrawScope.drawStinkLines(c: Offset, u: Float) {
    val green = Color(0xFF7FA650)
    listOf(-2.4f, 0f, 2.4f).forEach { dx ->
        val p = Path().apply {
            moveTo(c.x + dx * u, c.y - 4.6f * u)
            cubicTo(
                c.x + (dx - 0.5f) * u, c.y - 5.1f * u,
                c.x + (dx + 0.5f) * u, c.y - 5.5f * u,
                c.x + dx * u, c.y - 6.0f * u,
            )
        }
        drawPath(p, green, style = Stroke(width = u * 0.2f, cap = StrokeCap.Round))
    }
}
