package com.wizlabs.wizdipcharts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private val Amber = Color(0xFFFF9F0A)
private const val TICK_MS = 36L          // ~26 fps cap for the ripple loop
private const val SETTLE_AFTER_MS = 7000L // ripple calms to a still surface after this

/**
 * Side view of a horizontal capsule tank. The fuel rises to [level] (dip / max dip) with a
 * spring, the surface ripples for a few seconds after every change, then settles to a still
 * frame so an idle screen costs nothing. All paths and brushes are built once per size and
 * reused; the animation only invalidates the draw phase, never recomposition.
 */
@Composable
internal fun TankGauge(level: Float, pal: Pal, error: Boolean, modifier: Modifier = Modifier) {
    val target = level.coerceIn(0f, 1f)
    val lv = remember { Animatable(0f) }
    val amp = remember { Animatable(0f) }
    val phase = remember { mutableFloatStateOf(0f) }
    val errorState = rememberUpdatedState(error)
    val allowed = motionAllowed()

    LaunchedEffect(target) {
        lv.animateTo(target, spring(dampingRatio = 0.62f, stiffness = 60f))
    }
    LaunchedEffect(target, allowed) {
        if (!allowed) { amp.snapTo(0f); return@LaunchedEffect }
        val ticker = launch {
            val start = System.nanoTime()
            while (isActive) {
                androidx.compose.runtime.withFrameNanos { now -> phase.floatValue = ((now - start) / 1_000_000L).toFloat() / 1000f }
                delay(TICK_MS - 8L) // + vsync wait ≈ TICK_MS
            }
        }
        amp.animateTo(1f, tween(450))
        delay(SETTLE_AFTER_MS)
        amp.animateTo(0f, tween(2200, easing = LinearEasing))
        ticker.cancel()
    }

    Spacer(
        modifier.drawWithCache {
            val w = size.width
            val top = 14.dp.toPx()          // room for the dip hatch + rod above the shell
            val h = size.height - top
            val wall = 2.dp.toPx()
            val r = h / 2f
            val shell = Path().apply { addRoundRect(RoundRect(0f, top, w, top + h, CornerRadius(r))) }
            val fuel = Brush.verticalGradient(listOf(Yellow, Amber), startY = top, endY = top + h)
            val sheen = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), startY = top, endY = top + h * 0.45f)
            val front = Path()
            val back = Path()
            val maxAmp = 4.5.dp.toPx()
            val rodX = w * 0.5f
            val hatchW = 26.dp.toPx()
            val tick = 6.dp.toPx()
            val dot = 5.dp.toPx()

            fun wave(p: Path, surface: Float, a: Float, ph: Float, k: Float) {
                p.reset()
                p.moveTo(0f, top + h)
                val n = 28
                for (i in 0..n) {
                    val x = w * i / n
                    p.lineTo(x, surface + a * sin((x / w) * k * 2f * PI.toFloat() + ph))
                }
                p.lineTo(w, top + h)
                p.close()
            }

            onDrawBehind {
                val l = lv.value.coerceIn(0f, 1.02f)
                val t = phase.floatValue
                val edge = (sin(l.coerceIn(0f, 1f) * PI.toFloat()) * 0.7f + 0.3f)
                val a = maxAmp * amp.value * edge
                val surface = top + h * (1f - l)

                // Dip hatch on top and its rod.
                drawRoundRect(pal.raised, Offset(rodX - hatchW / 2, top - 7.dp.toPx()), Size(hatchW, 9.dp.toPx()), CornerRadius(3.dp.toPx()))
                clipPath(shell) {
                    drawRect(pal.field)
                    if (l > 0.001f) {
                        wave(back, surface - a * 0.4f, a * 0.8f, -t * 2.1f + 1.7f, 1.15f)
                        drawPath(back, Yellow.copy(alpha = 0.38f))
                        wave(front, surface, a, t * 2.6f, 1.6f)
                        drawPath(front, fuel)
                        // Rising bubbles while the surface is alive.
                        if (amp.value > 0.02f) {
                            for (i in 0 until 4) {
                                val speed = 0.18f + i * 0.05f
                                val prog = ((t * speed + i * 0.27f) % 1f)
                                val by = top + h - prog * (top + h - surface)
                                val bx = w * (0.18f + i * 0.21f) + sin(t * 3f + i) * 3.dp.toPx()
                                drawCircle(Color.White.copy(alpha = 0.35f * amp.value * (1f - prog)), (1.6f + i % 2).dp.toPx(), Offset(bx, by))
                            }
                        }
                    }
                    drawRect(sheen)
                    // Scale ticks every 10 % of the dip height.
                    for (i in 1..9) {
                        val y = top + h * i / 10f
                        val len = if (i == 5) tick * 1.8f else tick
                        drawLine(pal.label.copy(alpha = 0.22f), Offset(w - r * 0.35f - len, y), Offset(w - r * 0.35f, y), 1.dp.toPx())
                    }
                }
                drawPath(shell, if (errorState.value) pal.error else pal.separator, style = Stroke(wall))
                // Rod with the dip marker riding the surface.
                drawLine(pal.secondary.copy(alpha = 0.55f), Offset(rodX, top - 10.dp.toPx()), Offset(rodX, top + h - wall), 2.dp.toPx(), cap = StrokeCap.Round)
                if (l > 0.001f) {
                    val my = surface + a * sin((rodX / w) * 1.6f * 2f * PI.toFloat() + t * 2.6f)
                    drawCircle(pal.onAccent, dot + 1.5.dp.toPx(), Offset(rodX, my))
                    drawCircle(Yellow, dot, Offset(rodX, my))
                }
            }
        },
    )
}
