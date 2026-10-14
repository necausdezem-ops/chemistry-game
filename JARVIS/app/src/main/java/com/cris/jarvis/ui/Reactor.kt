package com.cris.jarvis.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/** Núcleo holográfico (reator ARC). `active` acelera o pulso quando o JARVIS fala/pensa. */
@Composable
fun ReactorCore(active: Boolean, modifier: Modifier = Modifier, tint: Color = Neon.Cyan) {
    val t = rememberInfiniteTransition(label = "reactor")
    val rot1 by t.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "r1")
    val rot2 by t.animateFloat(360f, 0f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "r2")
    val rot3 by t.animateFloat(0f, 360f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "r3")
    val pulse by t.animateFloat(
        0.88f, 1.12f,
        infiniteRepeatable(tween(if (active) 450 else 1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier.aspectRatio(1f)) {
        val c = center
        val r = size.minDimension / 2f

        // brilho de fundo
        drawCircle(
            brush = Brush.radialGradient(
                listOf(tint.copy(alpha = if (active) 0.45f else 0.28f), Color.Transparent),
                center = c, radius = r
            ),
            radius = r, center = c
        )

        // régua externa
        rotate(rot1, c) {
            for (i in 0 until 72) {
                val a = Math.toRadians(i * 5.0)
                val long = i % 6 == 0
                val r1 = r * 0.97f
                val r2 = r * (if (long) 0.89f else 0.93f)
                drawLine(
                    color = tint.copy(alpha = if (long) 0.95f else 0.4f),
                    start = Offset(c.x + (cos(a) * r1).toFloat(), c.y + (sin(a) * r1).toFloat()),
                    end = Offset(c.x + (cos(a) * r2).toFloat(), c.y + (sin(a) * r2).toFloat()),
                    strokeWidth = if (long) 3f else 1.5f
                )
            }
        }

        fun arc(f: Float, w: Float, start: Float, sweep: Float, a: Float, col: Color = tint) {
            val rr = r * f
            drawArc(
                color = col.copy(alpha = a),
                startAngle = start, sweepAngle = sweep, useCenter = false,
                topLeft = Offset(c.x - rr, c.y - rr), size = Size(rr * 2, rr * 2),
                style = Stroke(width = w, cap = StrokeCap.Round)
            )
        }

        rotate(rot2, c) {
            arc(0.80f, 7f, 0f, 70f, 0.95f)
            arc(0.80f, 7f, 120f, 70f, 0.95f)
            arc(0.80f, 7f, 240f, 70f, 0.95f)
        }
        rotate(rot3, c) {
            arc(0.64f, 3.5f, 20f, 110f, 0.85f, Neon.Blue)
            arc(0.64f, 3.5f, 200f, 110f, 0.85f, Neon.Blue)
        }
        arc(0.52f, 1.5f, 0f, 360f, 0.5f)

        // partículas orbitando
        for (k in 0 until 7) {
            val ang = Math.toRadians((rot3 * (1f + k * 0.18f) + k * 51f).toDouble())
            val orb = r * (0.46f + 0.07f * k)
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 2.5f + (k % 3),
                center = Offset(c.x + (cos(ang) * orb).toFloat(), c.y + (sin(ang) * orb).toFloat())
            )
        }

        // núcleo central
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White, tint, Color.Transparent),
                center = c, radius = r * 0.34f * pulse
            ),
            radius = r * 0.34f * pulse, center = c
        )
        val tri = Path().apply {
            val s = r * 0.20f
            moveTo(c.x, c.y - s)
            lineTo(c.x + s * 0.87f, c.y + s * 0.5f)
            lineTo(c.x - s * 0.87f, c.y + s * 0.5f)
            close()
        }
        drawPath(tri, Color.White.copy(alpha = 0.9f), style = Stroke(width = 3f))
    }
}
