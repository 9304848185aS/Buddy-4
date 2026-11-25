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

    fun ask(context: Context, prompt: String, reply: (String) -> Unit) {
        val prefs = context.getSharedPreferences("buddy_ai", Context.MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", "")?.trim().orEmpty()
        val model = prefs.getString("model", "")?.trim().orEmpty()
        val apiKey = prefs.getString("api_key", "")?.trim().orEmpty()

        if (endpoint.isBlank() || model.isBlank() || apiKey.isBlank()) {
            reply("मेरा AI brain अभी configure नहीं है। Buddy में AI Settings खोलकर provider का endpoint, model और API key सेव करें।")
            return
        }

        executor.execute {
            var result = "अभी AI सेवा से जवाब नहीं मिल पाया। इंटरनेट और API settings जाँचें।"
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 25000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                val messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content",
                        "You are Buddy, a helpful Hindi, English and Hinglish phone assistant. Answer clearly and concisely. You may explain how to do tasks, but you cannot directly control apps through this chat response. Never claim an action was completed unless Android confirms it. For medical, legal, financial or safety-critical topics, give general information and encourage qualified human help. Ask before sensitive actions."))
                    .put(JSONObject().put("role", "user").put("content", prompt))
                val body = JSONObject().put("model", model).put("messages", messages)
                    .put("temperature", 0.5).put("max_tokens", 500).toString()
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                if (code in 200..299) {
                    val content = JSONObject(response).optJSONArray("choices")
                        ?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
                    result = content.ifBlank { "AI से खाली जवाब मिला।" }
                } else {
                    result = when (code) {
                        401, 403 -> "API key सही नहीं है या इस सेवा की अनुमति नहीं है।"
                        429 -> "Free API की limit पूरी हो गई है। कुछ देर बाद फिर कोशिश करें।"
                        else -> "AI सेवा में error आया: HTTP $code"
                    }
                }
            } catch (_: Exception) {
                result = "AI से कनेक्ट नहीं हो पाया। इंटरनेट, endpoint और model जाँचें।"
            } finally {
                connection?.disconnect()
            }
            main.post { reply(result.take(1800)) }
        }
    }
}
