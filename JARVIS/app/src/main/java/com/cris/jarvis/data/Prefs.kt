package com.cris.jarvis.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cris.jarvis.ai.AiHub

/** Configurações do JARVIS. Chaves de API ficam em armazenamento criptografado (Keystore). */
class Prefs private constructor(ctx: Context) {

    private val sp: SharedPreferences = ctx.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    private val secure: SharedPreferences? = try {
        val mk = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            ctx, "jarvis_secure", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        null
    }
    private val memKeys = HashMap<String, String>()

    var replaceMode: Boolean
        get() = sp.getBoolean("replaceMode", true)
        set(v) = sp.edit().putBoolean("replaceMode", v).apply()

    var speakNotifications: Boolean
        get() = sp.getBoolean("speakNotifications", true)
        set(v) = sp.edit().putBoolean("speakNotifications", v).apply()

    var speakWhatsApp: Boolean
        get() = sp.getBoolean("speakWhatsApp", true)
        set(v) = sp.edit().putBoolean("speakWhatsApp", v).apply()

    var holoOverlay: Boolean
        get() = sp.getBoolean("holoOverlay", true)
        set(v) = sp.edit().putBoolean("holoOverlay", v).apply()

    var speakResponses: Boolean
        get() = sp.getBoolean("speakResponses", true)
        set(v) = sp.edit().putBoolean("speakResponses", v).apply()

    // ---- Provedores de IA ----
    fun baseUrl(id: String, def: String): String = sp.getString("url_$id", null) ?: def
    fun setBaseUrl(id: String, v: String) = sp.edit().putString("url_$id", v).apply()

    fun model(id: String): String = sp.getString("model_$id", "") ?: ""
    fun setModel(id: String, v: String) = sp.edit().putString("model_$id", v).apply()

    fun enabled(id: String): Boolean = sp.getBoolean("on_$id", false)
    fun setEnabled(id: String, v: Boolean) = sp.edit().putBoolean("on_$id", v).apply()

    fun apiKey(id: String): String = secure?.getString("key_$id", null) ?: memKeys[id] ?: ""
    fun setApiKey(id: String, v: String) {
        val s = secure
        if (s != null) s.edit().putString("key_$id", v).apply() else memKeys[id] = v
    }

    /** Ordem da cadeia de fallback (primeiro = prioridade máxima). */
    fun chainIds(): List<String> {
        val saved = (sp.getString("chain", null) ?: "").split(",").filter { it.isNotBlank() }
        val all = AiHub.catalog.map { it.id }
        return saved.filter { it in all } + all.filter { it !in saved }
    }

    fun moveUp(id: String) {
        val l = chainIds().toMutableList()
        val i = l.indexOf(id)
        if (i > 0) {
            l.removeAt(i)
            l.add(i - 1, id)
            sp.edit().putString("chain", l.joinToString(",")).apply()
        }
    }

    companion object {
        @Volatile
        private var inst: Prefs? = null

        fun get(ctx: Context): Prefs =
            inst ?: synchronized(this) {
                inst ?: Prefs(ctx.applicationContext).also { inst = it }
            }
    }
}
