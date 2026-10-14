package com.cris.jarvis.notify

import android.app.Notification
import android.content.ComponentName
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.cris.jarvis.data.Prefs
import com.cris.jarvis.voice.Speaker

/**
 * Lê as notificações de todos os apps (inclusive WhatsApp), mostra como hologramas,
 * fala em voz alta e, no modo holográfico, remove a notificação comum da barra.
 */
class JarvisNotificationListener : NotificationListenerService() {

    private lateinit var prefs: Prefs
    private var overlay: HoloOverlay? = null
    private val seen = LinkedHashSet<String>()

    private val whatsappPkgs = setOf("com.whatsapp", "com.whatsapp.w4b")

    // Categorias que nunca são tocadas: chamadas, alarmes, navegação, mídia, progresso etc.
    private val protectedCategories = setOf(
        Notification.CATEGORY_CALL,
        Notification.CATEGORY_ALARM,
        Notification.CATEGORY_NAVIGATION,
        Notification.CATEGORY_TRANSPORT,
        Notification.CATEGORY_PROGRESS,
        Notification.CATEGORY_SERVICE,
        Notification.CATEGORY_SYSTEM,
        Notification.CATEGORY_STATUS,
        Notification.CATEGORY_REMINDER
    )

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs.get(this)
        Speaker.init(this)
        NotificationHub.attach(this)
        overlay = HoloOverlay(this).also { it.start() }
    }

    override fun onListenerConnected() {
        NotificationHub.listenerConnected.value = true
    }

    override fun onListenerDisconnected() {
        NotificationHub.listenerConnected.value = false
        try {
            requestRebind(ComponentName(this, JarvisNotificationListener::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "rebind falhou")
        }
    }

    override fun onDestroy() {
        overlay?.stop()
        overlay = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            handle(sbn)
        } catch (e: Exception) {
            Log.w(TAG, "erro ao tratar notificação: ${e.javaClass.simpleName}")
        }
    }

    private fun isProtected(pkg: String, category: String?): Boolean {
        if (category != null && category in protectedCategories) return true
        return pkg == "android" ||
            pkg.startsWith("com.android.systemui") ||
            pkg.startsWith("com.android.phone") ||
            pkg.contains("dialer") ||
            pkg.contains("incallui") ||
            pkg.contains("telecom") ||
            pkg.contains("deskclock")
    }

    @Suppress("DEPRECATION")
    private fun handle(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val n = sbn.notification ?: return
        if (sbn.isOngoing || isProtected(sbn.packageName, n.category)) return
        val extras = n.extras ?: return

        // Só remove a notificação comum se o holograma flutuante puder realmente aparecer.
        val replaceActive = prefs.replaceMode && prefs.holoOverlay &&
            Settings.canDrawOverlays(this) && sbn.isClearable

        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            if (replaceActive) safeCancel(sbn.key)
            return
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        var body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()

        // Estilo de conversa (WhatsApp, Telegram etc.): pega a última mensagem e quem enviou
        val msgs = extras.getParcelableArray("android.messages")
        val last = msgs?.lastOrNull() as? Bundle
        val msgText = last?.getCharSequence("text")?.toString()?.trim()
        if (!msgText.isNullOrEmpty()) {
            val sender = last?.getCharSequence("sender")?.toString()?.trim()
            body = if (!sender.isNullOrEmpty() && sender != title) "$sender: $msgText" else msgText
        }

        val lower = body.lowercase()
        if (lower.contains("verificando novas mensagens") || lower.contains("checking for new messages")) return
        if (title.isBlank() && body.isBlank()) return

        val dedup = sbn.key + "|" + title.hashCode() + "|" + body.hashCode()
        val duplicate = synchronized(seen) {
            if (seen.contains(dedup)) {
                true
            } else {
                seen.add(dedup)
                if (seen.size > 300) seen.remove(seen.first())
                false
            }
        }
        if (duplicate) {
            if (replaceActive) safeCancel(sbn.key)
            return
        }

        val isWa = sbn.packageName in whatsappPkgs
        val app = appLabel(sbn.packageName)
        val jn = JNotif(
            id = "${sbn.key}|${sbn.postTime}|${body.hashCode()}",
            pkg = sbn.packageName,
            app = if (isWa) "WhatsApp" else app,
            title = title,
            text = body,
            time = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis(),
            isWhatsApp = isWa
        )

        val overlayOk = prefs.holoOverlay && Settings.canDrawOverlays(this)
        NotificationHub.post(jn, n.contentIntent, overlayOk)

        if (prefs.speakNotifications && (!isWa || prefs.speakWhatsApp)) {
            val parts = listOf(jn.app, jn.title, jn.text).filter { it.isNotBlank() }
            Speaker.speak(parts.joinToString(". "))
        }

        if (replaceActive) safeCancel(sbn.key)
    }

    private fun safeCancel(key: String) {
        try {
            cancelNotification(key)
        } catch (e: Exception) {
            Log.w(TAG, "não foi possível remover a notificação original")
        }
    }

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    companion object {
        private const val TAG = "JARVIS"
    }
}
