package com.cris.jarvis.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cris.jarvis.notify.JNotif
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Cartão holográfico de notificação (vidro + borda neon + cantos recortados). */
@Composable
fun HoloCard(
    n: JNotif,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    val a = remember(n.id) { Animatable(0f) }
    LaunchedEffect(n.id) { a.animateTo(1f, tween(450)) }
    val accent = if (n.isWhatsApp) Neon.Green else Neon.Cyan
    val time = remember(n.time) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(n.time)) }

    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = a.value
                translationY = (1f - a.value) * -48f
            }
            .clip(CutShape)
            .background(Brush.linearGradient(listOf(Color(0xEB0A2231), Color(0xEB030C14))))
            .border(1.dp, Brush.linearGradient(listOf(accent, Neon.Blue.copy(alpha = 0.5f))), CutShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f))
                        .border(1.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(n.app.take(1).uppercase(), color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    n.app.uppercase(),
                    color = accent,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(time, color = Neon.Dim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                if (onClose != null) {
                    Spacer(Modifier.width(12.dp))
                    Text("✕", color = Neon.Dim, fontSize = 14.sp, modifier = Modifier.clickable { onClose() })
                }
            }
            if (n.title.isNotBlank()) {
                Text(
                    n.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (n.text.isNotBlank()) {
                Text(
                    n.text,
                    color = Color(0xFFB8D8E0),
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}
