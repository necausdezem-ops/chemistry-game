package com.cris.jarvis.ai

import com.cris.jarvis.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class Protocol { OPENAI, ANTHROPIC }

data class Provider(
    val id: String,
    val name: String,
    val category: String,
    val protocol: Protocol,
    val baseUrl: String,
    val needsKey: Boolean
)

data class Reply(val provider: String, val text: String)

/** Hub de IA: vários provedores, roteador com cadeia de fallback. */
object AiHub {

    private const val SYSTEM =
        "Você é o J.A.R.V.I.S., assistente pessoal estilo Tony Stark. Responda sempre em português do Brasil, " +
            "de forma objetiva, elegante e levemente espirituosa. Nunca diga que executou uma ação que não executou."

    val catalog: List<Provider> = listOf(
        Provider("ollama", "Ollama (PC na rede)", "Local — rede", Protocol.OPENAI, "http://192.168.0.10:11434/v1", false),
        Provider("lmstudio", "LM Studio (PC na rede)", "Local — rede", Protocol.OPENAI, "http://192.168.0.10:1234/v1", false),
        Provider("deepseek", "DeepSeek", "Nuvem — custo-benefício", Protocol.OPENAI, "https://api.deepseek.com", true),
        Provider("openai", "OpenAI (ChatGPT)", "Nuvem — geral", Protocol.OPENAI, "https://api.openai.com/v1", true),
        Provider("anthropic", "Anthropic (Claude)", "Nuvem — geral", Protocol.ANTHROPIC, "https://api.anthropic.com/v1", true),
        Provider("gemini", "Google (Gemini)", "Nuvem — geral", Protocol.OPENAI, "https://generativelanguage.googleapis.com/v1beta/openai", true),
        Provider("grok", "xAI (Grok)", "Nuvem — geral", Protocol.OPENAI, "https://api.x.ai/v1", true),
        Provider("mistral", "Mistral", "Nuvem — europeia", Protocol.OPENAI, "https://api.mistral.ai/v1", true),
        Provider("groq", "Groq", "Nuvem — ultrarrápida", Protocol.OPENAI, "https://api.groq.com/openai/v1", true),
        Provider("perplexity", "Perplexity", "Nuvem — pesquisa web", Protocol.OPENAI, "https://api.perplexity.ai", true),
        Provider("openrouter", "OpenRouter", "Agregador (centenas de modelos)", Protocol.OPENAI, "https://openrouter.ai/api/v1", true),
        Provider("together", "Together AI", "Nuvem — open source", Protocol.OPENAI, "https://api.together.xyz/v1", true),
        Provider("custom", "Personalizado", "Qualquer servidor compatível com OpenAI", Protocol.OPENAI, "", false)
    )

    fun find(id: String): Provider? = catalog.firstOrNull { it.id == id }

    /** Tenta cada provedor ativo na ordem da cadeia até um responder. */
    suspend fun ask(prefs: Prefs, messages: List<Pair<String, String>>): Reply = withContext(Dispatchers.IO) {
        val chain = prefs.chainIds().mapNotNull { find(it) }.filter { prefs.enabled(it.id) }
        if (chain.isEmpty()) throw IllegalStateException("Nenhum provedor de IA ativo. Configure na aba IA.")
        val errors = mutableListOf<String>()
        for (p in chain) {
            try {
                return@withContext Reply(p.name, chat(prefs, p, messages))
            } catch (e: Exception) {
                errors.add("${p.name}: ${e.message}")
            }
        }
        throw IllegalStateException("Todos os provedores falharam:\n" + errors.joinToString("\n"))
    }

    suspend fun chatWith(prefs: Prefs, p: Provider, messages: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) { chat(prefs, p, messages) }

    suspend fun listModels(prefs: Prefs, p: Provider): List<String> = withContext(Dispatchers.IO) {
        val base = prefs.baseUrl(p.id, p.baseUrl).trim().trimEnd('/')
        require(base.isNotEmpty()) { "Endereço vazio" }
        val key = prefs.apiKey(p.id).trim()
        val headers = HashMap<String, String>()
        if (p.protocol == Protocol.ANTHROPIC) {
            headers["x-api-key"] = key
            headers["anthropic-version"] = "2023-06-01"
        } else if (key.isNotEmpty()) {
            headers["Authorization"] = "Bearer $key"
        }
        val res = JSONObject(http("GET", "$base/models", headers, null))
        val data = res.optJSONArray("data") ?: JSONArray()
        (0 until data.length())
            .map { data.getJSONObject(it).optString("id").removePrefix("models/") }
            .filter { it.isNotBlank() }
            .sorted()
    }

    private fun chat(prefs: Prefs, p: Provider, messages: List<Pair<String, String>>): String {
        val base = prefs.baseUrl(p.id, p.baseUrl).trim().trimEnd('/')
        val model = prefs.model(p.id).trim()
        val key = prefs.apiKey(p.id).trim()
        require(base.isNotEmpty()) { "Endereço vazio" }
        require(model.isNotEmpty()) { "Escolha um modelo (botão BUSCAR MODELOS)" }
        if (p.needsKey) require(key.isNotEmpty()) { "Falta a chave de API" }

        return when (p.protocol) {
            Protocol.OPENAI -> {
                val arr = JSONArray().put(JSONObject().put("role", "system").put("content", SYSTEM))
                messages.forEach { arr.put(JSONObject().put("role", it.first).put("content", it.second)) }
                val body = JSONObject().put("model", model).put("messages", arr).toString()
                val headers = hashMapOf("Content-Type" to "application/json")
                if (key.isNotEmpty()) headers["Authorization"] = "Bearer $key"
                val res = JSONObject(http("POST", "$base/chat/completions", headers, body))
                res.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content").trim()
            }

            Protocol.ANTHROPIC -> {
                val arr = JSONArray()
                messages.forEach { arr.put(JSONObject().put("role", it.first).put("content", it.second)) }
                val body = JSONObject()
                    .put("model", model)
                    .put("max_tokens", 1024)
                    .put("system", SYSTEM)
                    .put("messages", arr)
                    .toString()
                val headers = hashMapOf(
                    "Content-Type" to "application/json",
                    "x-api-key" to key,
                    "anthropic-version" to "2023-06-01"
                )
                val res = JSONObject(http("POST", "$base/messages", headers, body))
                val content = res.getJSONArray("content")
                val sb = StringBuilder()
                for (i in 0 until content.length()) {
                    val block = content.getJSONObject(i)
                    if (block.optString("type") == "text") sb.append(block.optString("text"))
                }
                sb.toString().trim()
            }
        }
    }

    private fun http(method: String, url: String, headers: Map<String, String>, body: String?): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 15000
            c.readTimeout = 90000
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (body != null) {
                c.doOutput = true
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IOException("HTTP $code ${text.take(240)}")
            return text
        } finally {
            c.disconnect()
        }
    }
}
