package com.cris.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.cris.jarvis.data.Prefs
import com.cris.jarvis.notify.NotificationHub
import com.cris.jarvis.ui.AiScreen
import com.cris.jarvis.ui.CoreScreen
import com.cris.jarvis.ui.HoloScreen
import com.cris.jarvis.ui.JarvisTheme
import com.cris.jarvis.ui.Neon
import com.cris.jarvis.ui.SettingsScreen
import com.cris.jarvis.ui.hudBackground
import com.cris.jarvis.voice.Speaker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        Speaker.init(this)
        NotificationHub.attach(this)
        val prefs = Prefs.get(this)
        setContent { JarvisTheme { JarvisRoot(prefs) } }
    }
}

@Composable
fun JarvisRoot(prefs: Prefs) {
    var tab by remember { mutableIntStateOf(0) }
    val names = listOf("NÚCLEO", "HOLO", "IA", "AJUSTES")
    val icons = listOf("◉", "▣", "✦", "⚙")

    Box(Modifier.fillMaxSize().hudBackground()) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Neon.Panel) {
                    names.forEachIndexed { i, n ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Text(icons[i]) },
                            label = { Text(n, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    0 -> CoreScreen(prefs)
                    1 -> HoloScreen()
                    2 -> AiScreen(prefs)
                    else -> SettingsScreen(prefs)
                }
            }
        }
    }
}
