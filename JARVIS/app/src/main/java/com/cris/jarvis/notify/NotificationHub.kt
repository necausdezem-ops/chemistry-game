package com.cris.jarvis.notify

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class JNotif(
    val id: String,
    val pkg: String,
    val app: String,
    val title: String,
    val text: String,
    val time: Long,
    val isWhatsApp: Boolean
)

/** Central única: histórico de cartões, fila do holograma flutuante e ações de abrir. */
object NotificationHub {

    private val lock = Any()
    private val _items = MutableStateFlow<List<JNotif>>(emptyList())
    val items: StateFlow<List<JNotif>> = _items.asStateFlow()

    private val _current = MutableStateFlow<JNotif?>(null)
    val current: StateFlow<JNotif?> = _current.asStateFlow()

    val listenerConnected = MutableStateFlow(false)

    private val queue = ArrayDeque<JNotif>()
    private val intents = ConcurrentHashMap<String, PendingIntent>()
    private var appCtx: Context? = null
    private var restored = false

    fun attach(ctx: Context) {
        synchronized(lock) {
            appCtx = ctx.applicationContext
            if (!restored) {
                restored = true
                restore()
            }
        }
    }

    fun post(n: JNotif, pi: PendingIntent?, overlay: Boolean) {
        synchronized(lock) {
            _items.value = (listOf(n) + _items.value).take(100)
            if (pi != null) intents[n.id] = pi
            val alive = _items.value.map { it.id }.toSet()
            intents.keys.retainAll(alive)
            if (overlay) {
                queue.addLast(n)
                while (queue.size > 5) queue.removeAt(1)
                if (_current.value == null) _current.value = queue.first()
            }
        }
        persist()
    }

    fun dismissCurrent() {
        synchronized(lock) {
            if (queue.isNotEmpty()) queue.removeFirst()
            _current.value = queue.firstOrNull()
        }
    }

    fun clear() {
        synchronized(lock) {
            _items.value = emptyList()
            queue.clear()
            _current.value = null
            intents.clear()
        }
        persist()
    }

    /** Abre o app/conversa de origem da notificação. */
    fun open(ctx: Context, n: JNotif): Boolean {
        val pi = intents[n.id]
        if (pi != null) {
            try {
                val opts = ActivityOptions.makeBasic()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    opts.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
                pi.send(ctx, 0, null, null, null, null, opts.toBundle())
                return true
            } catch (e: Exception) {
                // cai para a abertura simples do app
            }
        }
        val launch = ctx.packageManager.getLaunchIntentForPackage(n.pkg) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(launch)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun persist() {
        val ctx = appCtx ?: return
        try {
            val arr = JSONArray()
            _items.value.forEach {
                arr.put(
                    JSONObject().put("id", it.id).put("pkg", it.pkg).put("app", it.app)
                        .put("title", it.title).put("text", it.text).put("time", it.time)
                        .put("wa", it.isWhatsApp)
                )
            }
            ctx.getSharedPreferences("jarvis_hub", Context.MODE_PRIVATE)
                .edit().putString("items", arr.toString()).apply()
        } catch (e: Exception) {
            // histórico é opcional; ignora falhas
        }
    }

    private fun restore() {
        val ctx = appCtx ?: return
        try {
            val raw = ctx.getSharedPreferences("jarvis_hub", Context.MODE_PRIVATE).getString("items", null) ?: return
            val arr = JSONArray(raw)
            val list = (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                JNotif(
                    o.getString("id"), o.getString("pkg"), o.getString("app"),
                    o.getString("title"), o.getString("text"), o.getLong("time"), o.optBoolean("wa")
                )
            }
            _items.value = list
        } catch (e: Exception) {
            // ignora histórico corrompido
        }
    }
}
