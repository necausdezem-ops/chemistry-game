package com.cris.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Neon {
    val Cyan = Color(0xFF00E5FF)
    val Blue = Color(0xFF2979FF)
    val Amber = Color(0xFFFFB300)
    val Green = Color(0xFF00FF9C)
    val Red = Color(0xFFFF1744)
    val Bg = Color(0xFF020409)
    val Panel = Color(0xFF07131C)
    val Text = Color(0xFFE6FBFF)
    val Dim = Color(0xFF8FB4BF)
}

/** Painel com cantos recortados estilo HUD. */
val CutShape = GenericShape { size, _ ->
    val c = 22f
    moveTo(c, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - c)
    lineTo(size.width - c, size.height)
    lineTo(0f, size.height)
    lineTo(0f, c)
    close()
}

fun Modifier.hudBackground(): Modifier = this
    .background(
        Brush.verticalGradient(
            listOf(Color(0xFF020409), Color(0xFF04121C), Color(0xFF020409))
        )
    )
    .drawBehind {
        val step = 56f
        val line = Color(0x1400E5FF)
        var x = 0f
        while (x < size.width) {
            drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
    }

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon.Cyan,
            onPrimary = Color.Black,
            secondary = Neon.Blue,
            background = Neon.Bg,
            surface = Neon.Panel,
            onSurface = Neon.Text,
            onBackground = Neon.Text
        ),
        content = content
    )
}
