package com.cris.jarvis.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

/** Voz do JARVIS (TTS do Android em português do Brasil). */
object Speaker : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = mutableListOf<String>()
    val speaking = MutableStateFlow(false)

    @Synchronized
    fun init(ctx: Context) {
        if (tts != null) return
        tts = TextToSpeech(ctx.applicationContext, this)
    }

    @Synchronized
    override fun onInit(status: Int) {
        val t = tts ?: return
        if (status != TextToSpeech.SUCCESS) return
        t.setLanguage(Locale("pt", "BR"))
        t.setSpeechRate(1.0f)
        t.setPitch(0.85f)
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                speaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                speaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                speaking.value = false
            }
        })
        ready = true
        pending.forEach { t.speak(it, TextToSpeech.QUEUE_ADD, null, System.nanoTime().toString()) }
        pending.clear()
    }

    @Synchronized
    fun speak(text: String) {
        val clean = text.replace(Regex("\\s+"), " ").trim().take(400)
        if (clean.isEmpty()) return
        val t = tts
        if (t == null || !ready) {
            pending.add(clean)
            return
        }
        t.speak(clean, TextToSpeech.QUEUE_ADD, null, System.nanoTime().toString())
    }

    @Synchronized
    fun stop() {
        pending.clear()
        tts?.stop()
        speaking.value = false
    }
}
