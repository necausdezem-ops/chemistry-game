package com.cris.jarvis.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.cris.jarvis.ai.AiHub
import com.cris.jarvis.ai.Provider
import com.cris.jarvis.data.Prefs
import com.cris.jarvis.notify.JNotif
import com.cris.jarvis.notify.NotificationHub
import com.cris.jarvis.voice.Speaker
import kotlinx.coroutines.launch

// ---------- peças comuns ----------

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CutShape)
            .background(Neon.Panel.copy(alpha = 0.88f))
            .border(1.dp, Neon.Cyan.copy(alpha = 0.35f), CutShape)
            .padding(14.dp),
        content = content
    )
}

@Composable
fun Title(text: String) {
    Text(
        text,
        color = Neon.Cyan,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 3.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun StatusChip(label: String, ok: Boolean) {
    val col = if (ok) Neon.Green else Neon.Amber
    Text(
        "$label: ${if (ok) "ONLINE" else "OFFLINE"}",
        color = col,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier = Modifier
            .border(1.dp, col.copy(alpha = 0.6f), RoundedCornerShapeSmall)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private val RoundedCornerShapeSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)

@Composable
private fun SettingToggle(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Neon.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(desc, color = Neon.Dim, fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------- NÚCLEO ----------

data class ChatMsg(val role: String, val text: String, val via: String?)

@Composable
fun CoreScreen(prefs: Prefs) {
    val speaking by Speaker.speaking.collectAsState()
    val connected by NotificationHub.listenerConnected.collectAsState()
    var thinking by remember { mutableStateOf(false) }
    val msgs = remember { mutableStateListOf<ChatMsg>() }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.lastIndex)
    }

    val send = {
        val q = input.trim()
        if (q.isNotEmpty() && !thinking) {
            input = ""
            msgs.add(ChatMsg("user", q, null))
            thinking = true
            scope.launch {
                try {
                    val history = msgs.filter { it.role == "user" || it.role == "assistant" }.map { it.role to it.text }
                    val r = AiHub.ask(prefs, history)
                    msgs.add(ChatMsg("assistant", r.text, r.provider))
                    if (prefs.speakResponses) Speaker.speak(r.text)
                } catch (e: Exception) {
                    msgs.add(ChatMsg("error", e.message ?: "Erro desconhecido", null))
                } finally {
                    thinking = false
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            "J.A.R.V.I.S.",
            color = Neon.Cyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 6.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ReactorCore(
                active = speaking || thinking,
                tint = if (thinking) Neon.Amber else Neon.Cyan,
                modifier = Modifier.fillMaxWidth(0.62f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("NOTIFICAÇÕES", connected)
            StatusChip("VOZ", !speaking)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs) { m ->
                val mine = m.role == "user"
                val col = when (m.role) {
                    "user" -> Neon.Blue
                    "error" -> Neon.Red
                    else -> Neon.Cyan
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, col.copy(alpha = 0.5f), CutShape)
                        .padding(10.dp)
                ) {
                    Text(
                        if (mine) "VOCÊ" else if (m.role == "error") "FALHA" else "JARVIS · ${m.via ?: ""}",
                        color = col, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
                    )
                    Text(m.text, color = Neon.Text, fontSize = 14.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Fale com o JARVIS…") },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { send() }, enabled = !thinking) { Text(if (thinking) "…" else "ENVIAR") }
        }
        TextButton(onClick = { Speaker.stop() }) { Text("PARAR VOZ", color = Neon.Amber, fontSize = 11.sp) }
    }
}

// ---------- HOLO (central de notificações) ----------

@Composable
fun HoloScreen() {
    val ctx = LocalContext.current
    val items by NotificationHub.items.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Title("CENTRAL HOLOGRÁFICA") }
            TextButton(onClick = { NotificationHub.clear() }) { Text("LIMPAR", color = Neon.Amber, fontSize = 11.sp) }
        }
        if (items.isEmpty()) {
            Text(
                "Nenhuma notificação ainda. Ative o acesso às notificações na aba AJUSTES e envie um teste.",
                color = Neon.Dim, fontSize = 13.sp
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { n ->
                HoloCard(n, onClick = { NotificationHub.open(ctx, n) })
            }
        }
    }
}

// ---------- IA (hub de provedores) ----------

@Composable
fun AiScreen(prefs: Prefs) {
    var chain by remember { mutableStateOf(prefs.chainIds()) }
    val providers = chain.mapNotNull { AiHub.find(it) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Title("HUB DE IA")
        Text(
            "Os provedores ativados são tentados de cima para baixo (fallback). Local primeiro, nuvem como reforço.",
            color = Neon.Dim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(providers, key = { it.id }) { p ->
                ProviderCard(p, prefs) {
                    prefs.moveUp(p.id)
                    chain = prefs.chainIds()
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(p: Provider, prefs: Prefs, onMoveUp: () -> Unit) {
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(prefs.enabled(p.id)) }
    var open by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf(prefs.baseUrl(p.id, p.baseUrl)) }
    var model by remember { mutableStateOf(prefs.model(p.id)) }
    var key by remember { mutableStateOf(prefs.apiKey(p.id)) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable { open = !open }) {
                Text(p.name, color = Neon.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(p.category + if (open) "  ▲" else "  ▼", color = Neon.Dim, fontSize = 11.sp)
            }
            Switch(checked = enabled, onCheckedChange = { enabled = it; prefs.setEnabled(p.id, it) })
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = url, onValueChange = { url = it; prefs.setBaseUrl(p.id, it) },
                label = { Text("Endereço base") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = model, onValueChange = { model = it; prefs.setModel(p.id, it) },
                label = { Text("Modelo") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            if (p.needsKey || p.id == "custom") {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = key, onValueChange = { key = it; prefs.setApiKey(p.id, it) },
                    label = { Text("Chave de API (guardada criptografada)") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
                )
            }
            if (models.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Toque para escolher o modelo:", color = Neon.Dim, fontSize = 11.sp)
                models.take(10).forEach { m ->
                    Text(
                        m, color = Neon.Cyan, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth().clickable { model = m; prefs.setModel(p.id, m) }.padding(vertical = 4.dp)
                    )
                }
                if (models.size > 10) Text("…e mais ${models.size - 10} (digite o nome no campo Modelo)", color = Neon.Dim, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true; status = "Buscando…"
                        try {
                            models = AiHub.listModels(prefs, p)
                            status = "${models.size} modelos encontrados"
                        } catch (e: Exception) {
                            status = "Erro: ${e.message}"
                        } finally { busy = false }
                    }
                }) { Text("MODELOS", fontSize = 11.sp) }
                OutlinedButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true; status = "Testando…"
                        val t0 = System.currentTimeMillis()
                        try {
                            val r = AiHub.chatWith(prefs, p, listOf("user" to "Responda apenas: ok"))
                            status = "OK em ${System.currentTimeMillis() - t0} ms — \"${r.take(40)}\""
                        } catch (e: Exception) {
                            status = "Falhou: ${e.message}"
                        } finally { busy = false }
                    }
                }) { Text("TESTAR", fontSize = 11.sp) }
                OutlinedButton(onClick = onMoveUp) { Text("↑ PRIORIDADE", fontSize = 11.sp) }
            }
            if (status.isNotEmpty()) {
                Text(status, color = if (status.startsWith("OK") || status.contains("encontrados")) Neon.Green else Neon.Amber,
                    fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

// ---------- AJUSTES ----------

@Composable
fun SettingsScreen(prefs: Prefs) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }

    val listenerOn = tick >= 0 && NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    val overlayOn = tick >= 0 && Settings.canDrawOverlays(ctx)
    val battOn = tick >= 0 &&
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)

    var replace by remember { mutableStateOf(prefs.replaceMode) }
    var speakN by remember { mutableStateOf(prefs.speakNotifications) }
    var speakW by remember { mutableStateOf(prefs.speakWhatsApp) }
    var holo by remember { mutableStateOf(prefs.holoOverlay) }
    var speakR by remember { mutableStateOf(prefs.speakResponses) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Title("PERMISSÕES")
        Panel {
            PermRow("Acesso às notificações (ler tudo, inclusive WhatsApp)", listenerOn, "ATIVAR") {
                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            Spacer(Modifier.height(8.dp))
            PermRow("Exibir sobre outros apps (hologramas flutuantes)", overlayOn, "ATIVAR") {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            Spacer(Modifier.height(8.dp))
            PermRow("Sem restrição de bateria (para ficar vivo em segundo plano)", battOn, "ATIVAR") {
                ctx.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            Text(
                "Dica: se o Android bloquear a permissão de notificações (app instalado fora da Play Store), abra " +
                    "Configurações › Apps › JARVIS › menu ⋮ › “Permitir configurações restritas” e tente de novo.",
                color = Neon.Dim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)
            )
        }

        Title("MODO HOLOGRÁFICO")
        Panel {
            SettingToggle(
                "Substituir notificações normais",
                "Remove a notificação comum da barra depois de mostrar o holograma. Só age se a permissão de sobreposição estiver ativa. Chamadas, alarmes, navegação e mídia nunca são tocados.",
                replace
            ) { replace = it; prefs.replaceMode = it }
            SettingToggle("Hologramas flutuantes", "Cartões sobre qualquer app por alguns segundos; toque para abrir a conversa.", holo) {
                holo = it; prefs.holoOverlay = it
            }
            SettingToggle("Ler notificações em voz alta", "Fala app, título e texto de cada notificação.", speakN) {
                speakN = it; prefs.speakNotifications = it
            }
            SettingToggle("Ler mensagens do WhatsApp", "Fala quem enviou e o texto da mensagem (o que aparece na notificação).", speakW) {
                speakW = it; prefs.speakWhatsApp = it
            }
            SettingToggle("Falar respostas da IA", "O JARVIS responde em voz alta no chat.", speakR) {
                speakR = it; prefs.speakResponses = it
            }
        }

        Title("TESTE")
        Panel {
            Button(onClick = {
                val n = JNotif(
                    id = "teste-${System.nanoTime()}", pkg = "com.whatsapp", app = "WhatsApp",
                    title = "Contato de Teste", text = "Senhor, esta é uma mensagem holográfica de teste.",
                    time = System.currentTimeMillis(), isWhatsApp = true
                )
                NotificationHub.post(n, null, prefs.holoOverlay && Settings.canDrawOverlays(ctx))
                if (prefs.speakNotifications && prefs.speakWhatsApp) Speaker.speak("WhatsApp. ${n.title}. ${n.text}")
            }) { Text("ENVIAR HOLOGRAMA DE TESTE") }
            Text(
                "O holograma flutuante só aparece depois que o acesso às notificações estiver ativo (o serviço é quem hospeda a janela).",
                color = Neon.Dim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun PermRow(label: String, ok: Boolean, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Neon.Text, fontSize = 13.sp)
            Text(if (ok) "CONCEDIDA" else "PENDENTE", color = if (ok) Neon.Green else Neon.Amber, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        if (!ok) OutlinedButton(onClick = onClick, border = BorderStroke(1.dp, Neon.Cyan)) { Text(action, fontSize = 11.sp) }
    }
}
