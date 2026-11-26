package com.example.buddy

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.Locale

object AiBrain {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private const val PREFS = "buddy_ai"
    private const val HISTORY_KEY = "conversation_history"
    private const val MAX_HISTORY_MESSAGES = 10

    fun ask(context: Context, prompt: String, reply: (String) -> Unit) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", "")?.trim().orEmpty()
        val model = prefs.getString("model", "")?.trim().orEmpty()
        val apiKey = SecureKeyStore.get(appContext).trim()
        val remember = prefs.getBoolean("memory_enabled", false)

        if (endpoint.isBlank() || model.isBlank() || apiKey.isBlank()) {
            main.post { reply("मेरा AI brain अभी configure नहीं है। Buddy में AI Settings खोलकर endpoint, model और API key सेव करें।") }
            return
        }
        if (!NetworkSecurity.isSafeHttpsEndpoint(endpoint)) {
            main.post { reply("AI endpoint सुरक्षित HTTPS address होना चाहिए।") }
            return
        }

        executor.execute {
            var result = "अभी AI सेवा से जवाब नहीं मिल पाया। इंटरनेट और API settings जाँचें।"
            var connection: HttpURLConnection? = null
            try {
                val messages = JSONArray().put(
                    JSONObject().put("role", "system").put(
                        "content",
                        "You are Buddy, a helpful Hindi, English and Hinglish phone assistant. Answer clearly and concisely. You can explain tasks, but cannot directly control apps through a chat response. Never claim an action was completed unless Android confirms it. For medical, legal, financial or safety-critical topics, give general information and encourage qualified human help. Ask before sensitive actions."
                    )
                )
                if (remember) {
                    val saved = prefs.getString(HISTORY_KEY, "[]").orEmpty()
                    val history = try { JSONArray(saved) } catch (_: Exception) { JSONArray() }
                    for (i in 0 until history.length()) {
                        val item = history.optJSONObject(i) ?: continue
                        val role = item.optString("role")
                        val content = item.optString("content")
                        if ((role == "user" || role == "assistant") && content.isNotBlank()) {
                            messages.put(JSONObject().put("role", role).put("content", content))
                        }
                    }
                }
                messages.put(JSONObject().put("role", "user").put("content", prompt))

                connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 20000
                    instanceFollowRedirects = false
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                val body = JSONObject().put("model", model).put("messages", messages)
                    .put("temperature", 0.5).put("max_tokens", 500).toString()
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.use { NetworkSecurity.readUtf8Limited(it, 64 * 1024) }.orEmpty()
                if (code in 200..299) {
                    val content = JSONObject(response).optJSONArray("choices")
                        ?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
                    result = content.ifBlank { "AI से खाली जवाब मिला।" }
                    if (remember && content.isNotBlank()) {
                        val saved = prefs.getString(HISTORY_KEY, "[]").orEmpty()
                        val history = try { JSONArray(saved) } catch (_: Exception) { JSONArray() }
                        history.put(JSONObject().put("role", "user").put("content", prompt.take(1200)))
                        history.put(JSONObject().put("role", "assistant").put("content", content.take(1800)))
                        val trimmed = JSONArray()
                        val start = (history.length() - MAX_HISTORY_MESSAGES).coerceAtLeast(0)
                        for (i in start until history.length()) trimmed.put(history.getJSONObject(i))
                        prefs.edit().putString(HISTORY_KEY, trimmed.toString()).apply()
                    }
                } else {
                    result = when (code) {
                        401, 403 -> "API key सही नहीं है या इस सेवा की अनुमति नहीं है।"
                        429 -> "API की limit पूरी हो गई है। कुछ देर बाद फिर कोशिश करें।"
                        else -> "AI सेवा में error आया: HTTP $code"
                    }
                }
            } catch (_: Exception) {
                result = "AI से कनेक्ट नहीं हो पाया। इंटरनेट, endpoint और model जाँचें।"
            } finally {
                connection?.disconnect()
            }
            val finalResult = result.take(1800)
            main.post { reply(finalResult) }
        }
    }


    data class ActionStep(val action: String, val value: String)
    data class ActionPlan(val steps: List<ActionStep>, val reason: String)

    fun planAction(context: Context, prompt: String, reply: (ActionPlan) -> Unit) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", "")?.trim().orEmpty()
        val model = prefs.getString("model", "")?.trim().orEmpty()
        val apiKey = SecureKeyStore.get(appContext).trim()
        if (endpoint.isBlank() || model.isBlank() || apiKey.isBlank() ||
            !NetworkSecurity.isSafeHttpsEndpoint(endpoint)) {
            main.post { reply(ActionPlan(emptyList(), "AI settings are not configured")) }
            return
        }

        executor.execute {
            var plan = ActionPlan(emptyList(), "No safe supported action")
            var connection: HttpURLConnection? = null
            try {
                val system = """
                    You are the action planner for an Android voice assistant.
                    Return ONLY one JSON object with exactly these fields:
                    {"steps":[{"action":"OPEN_APP|TYPE_TEXT|WEB_SEARCH","value":"..."}],"reason":"..."}
                    Plan one to three steps only when the user's request clearly needs a sequence.
                    OPEN_APP: open an installed app by its visible app name.
                    TYPE_TEXT: insert exact dictated text only into a text field the user has already focused.
                    WEB_SEARCH: open a web search for the given query.
                    Each step must be independently understandable and safe. Use only these three actions.
                    Never plan calls, messages, sending, posting, deleting, purchases, payments, banking,
                    passwords, private photos/videos, permission changes, form submission, or arbitrary taps.
                    Never infer consent. The app will ask for separate yes/no confirmation before EVERY step.
                    If the request cannot be safely represented, return {"steps":[],"reason":"..."}.
                    Keep each value under 180 characters. No markdown or extra text.
                """.trimIndent()
                val messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", prompt.take(1000)))
                connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 20000
                    instanceFollowRedirects = false
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                val body = JSONObject().put("model", model).put("messages", messages)
                    .put("temperature", 0.0).put("max_tokens", 260).toString()
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                if (connection.responseCode in 200..299) {
                    val response = connection.inputStream.use { NetworkSecurity.readUtf8Limited(it, 64 * 1024) }
                    val content = JSONObject(response).optJSONArray("choices")
                        ?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
                    val startJson = content.indexOf('{')
                    val endJson = content.lastIndexOf('}')
                    if (startJson >= 0 && endJson > startJson) {
                        val json = JSONObject(content.substring(startJson, endJson + 1))
                        val array = json.optJSONArray("steps") ?: JSONArray()
                        val safeSteps = mutableListOf<ActionStep>()
                        if (array.length() in 1..3) {
                            for (i in 0 until array.length()) {
                                val item = array.optJSONObject(i) ?: break
                                val action = item.optString("action").uppercase(Locale.ROOT)
                                val value = item.optString("value").trim()
                                if (action !in setOf("OPEN_APP", "TYPE_TEXT", "WEB_SEARCH") ||
                                    value.isBlank() || value.length > 180) {
                                    safeSteps.clear()
                                    break
                                }
                                safeSteps.add(ActionStep(action, value))
                            }
                        }
                        if (safeSteps.isNotEmpty() && safeSteps.size == array.length()) {
                            plan = ActionPlan(safeSteps, json.optString("reason").take(120))
                        }
                    }
                }
            } catch (_: Exception) {
                plan = ActionPlan(emptyList(), "Planner unavailable")
            } finally {
                connection?.disconnect()
            }
            val finalPlan = plan
            main.post { reply(finalPlan) }
        }
    }

    fun clearHistory(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(HISTORY_KEY).apply()
    }
}
