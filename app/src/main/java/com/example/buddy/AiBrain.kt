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
        val apiKey = prefs.getString("api_key", "")?.trim().orEmpty()
        val remember = prefs.getBoolean("memory_enabled", false)

        if (endpoint.isBlank() || model.isBlank() || apiKey.isBlank()) {
            main.post { reply("मेरा AI brain अभी configure नहीं है। Buddy में AI Settings खोलकर endpoint, model और API key सेव करें।") }
            return
        }
        if (!endpoint.startsWith("https://", ignoreCase = true)) {
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
                    connectTimeout = 15000
                    readTimeout = 25000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                val body = JSONObject().put("model", model).put("messages", messages)
                    .put("temperature", 0.5).put("max_tokens", 500).toString()
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
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


    data class ActionPlan(val action: String, val value: String, val reason: String)

    fun planAction(context: Context, prompt: String, reply: (ActionPlan) -> Unit) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", "")?.trim().orEmpty()
        val model = prefs.getString("model", "")?.trim().orEmpty()
        val apiKey = prefs.getString("api_key", "")?.trim().orEmpty()
        if (endpoint.isBlank() || model.isBlank() || apiKey.isBlank() ||
            !endpoint.startsWith("https://", ignoreCase = true)) {
            main.post { reply(ActionPlan("NONE", "", "AI settings are not configured")) }
            return
        }

        executor.execute {
            var plan = ActionPlan("NONE", "", "No safe supported action")
            var connection: HttpURLConnection? = null
            try {
                val system = """
                    You are the action planner for an Android voice assistant.
                    Return ONLY one valid JSON object with exactly these string fields:
                    {"action":"OPEN_APP|TYPE_TEXT|WEB_SEARCH|NONE","value":"...","reason":"..."}
                    Choose OPEN_APP only when the user clearly asks to open an app; value must be the visible app name, not a package name.
                    Choose TYPE_TEXT only when the user clearly asks to type dictated text into the text field they have already focused; value is only the exact text to type.
                    Choose WEB_SEARCH only when the user clearly asks to search the web; value is only the search query.
                    Otherwise choose NONE.
                    Never plan calls, messages, sending, posting, deleting, purchases, payments, banking, passwords, private photos/videos, permission changes, or form submission.
                    Never chain actions. Never infer consent. Keep value under 180 characters. No markdown or extra text.
                """.trimIndent()
                val messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", prompt.take(1000)))
                connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 25000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                val body = JSONObject().put("model", model).put("messages", messages)
                    .put("temperature", 0.0).put("max_tokens", 180).toString()
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                if (connection.responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val content = JSONObject(response).optJSONArray("choices")
                        ?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
                    val start = content.indexOf('{')
                    val end = content.lastIndexOf('}')
                    if (start >= 0 && end > start) {
                        val json = JSONObject(content.substring(start, end + 1))
                        val action = json.optString("action").uppercase(Locale.ROOT)
                        val value = json.optString("value").trim()
                        val reason = json.optString("reason").take(120)
                        if (action in setOf("OPEN_APP", "TYPE_TEXT", "WEB_SEARCH") &&
                            value.isNotBlank() && value.length <= 180) {
                            plan = ActionPlan(action, value, reason)
                        }
                    }
                }
            } catch (_: Exception) {
                plan = ActionPlan("NONE", "", "Planner unavailable")
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
