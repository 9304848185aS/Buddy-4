package com.example.buddy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var endpointInput: EditText
    private lateinit var modelInput: EditText
    private lateinit var keyInput: EditText
    private lateinit var memoryCheck: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("buddy_ai", MODE_PRIVATE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }
        fun label(value: String) = TextView(this).apply {
            text = value
            textSize = 15f
            setPadding(0, 12, 0, 4)
        }
        fun field(hintText: String, value: String, type: Int = InputType.TYPE_CLASS_TEXT) =
            EditText(this).apply {
                hint = hintText
                setText(value)
                textSize = 15f
                inputType = type
                setSingleLine(true)
            }

        content.addView(TextView(this).apply {
            text = "BUDDY"
            textSize = 34f
            gravity = Gravity.CENTER
        })
        status = TextView(this).apply {
            text = "Phone assistant ready. Start listening and speak a command."
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 12)
        }
        content.addView(status)
        content.addView(Button(this).apply {
            text = "START LISTENING"
            setOnClickListener { requestAndStart() }
        })
        content.addView(Button(this).apply {
            text = "STOP"
            setOnClickListener {
                stopService(Intent(this@MainActivity, BuddyListeningService::class.java))
                status.text = "Stopped"
            }
        })

        content.addView(Button(this).apply {
            text = "ENABLE OPTIONAL TEXT TYPING"
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Android Accessibility settings will open. Enable Buddy only if you want voice typing into the field you have focused.",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })
        content.addView(TextView(this).apply {
            text = "Typing permission is optional. Buddy can only place your spoken text into the text field you have already focused. It does not read screen contents, tap buttons, submit forms, or send messages."
            textSize = 13f
            setPadding(0, 4, 0, 12)
        })

        content.addView(label("AI BRAIN SETTINGS (OpenAI-compatible API)"))
        content.addView(label("API endpoint"))
        endpointInput = field("https://api.groq.com/openai/v1/chat/completions",
            prefs.getString("endpoint", "https://api.groq.com/openai/v1/chat/completions").orEmpty())
        content.addView(endpointInput)
        content.addView(label("Model name"))
        modelInput = field("Enter the model ID from your provider",
            prefs.getString("model", "").orEmpty())
        content.addView(modelInput)
        content.addView(label("API key (stored only on this phone)"))
        keyInput = field("Paste provider API key here",
            prefs.getString("api_key", "").orEmpty(),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        content.addView(keyInput)
        content.addView(Button(this).apply {
            text = "SAVE AI SETTINGS"
            setOnClickListener {
                val endpoint = endpointInput.text.toString().trim()
                val model = modelInput.text.toString().trim()
                val key = keyInput.text.toString().trim()
                if (endpoint.isNotBlank() && !endpoint.startsWith("https://", ignoreCase = true)) {
                    Toast.makeText(this@MainActivity, "Use a secure https:// endpoint.", Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putString("endpoint", endpoint)
                        .putString("model", model).putString("api_key", key)
                        .putBoolean("memory_enabled", memoryCheck.isChecked).apply()
                    Toast.makeText(this@MainActivity, "AI settings saved on this phone.", Toast.LENGTH_SHORT).show()
                }
            }
        })

        content.addView(label("OPTIONAL CONVERSATION MEMORY"))
        memoryCheck = CheckBox(this).apply {
            text = "Remember recent AI chat on this phone"
            isChecked = prefs.getBoolean("memory_enabled", false)
        }
        content.addView(memoryCheck)
        content.addView(TextView(this).apply {
            text = "Memory is OFF by default. If enabled, Buddy stores up to 5 recent exchanges locally on this phone and sends that context with your next AI question. Do not store passwords, payment details, or private information."
            textSize = 13f
            setPadding(0, 4, 0, 8)
        })
        content.addView(Button(this).apply {
            text = "CLEAR AI CHAT MEMORY"
            setOnClickListener {
                AiBrain.clearHistory(this@MainActivity)
                Toast.makeText(this@MainActivity, "Recent AI chat memory cleared.", Toast.LENGTH_SHORT).show()
            }
        })
        content.addView(TextView(this).apply {
            text = "AI chat requires internet and a provider API key. Free quotas vary. Never share your key or commit it to GitHub. Voice commands for opening apps, settings, alarms and web search work without an AI key. Memory preference is saved when you tap SAVE AI SETTINGS."
            textSize = 13f
            setPadding(0, 12, 0, 16)
        })
        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)
    }

    private fun requestAndStart() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 7)
        else startBuddy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 7 && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) startBuddy()
        else Toast.makeText(this, "Microphone permission is needed to listen.", Toast.LENGTH_LONG).show()
    }

    private fun startBuddy() {
        try {
            val i = Intent(this, BuddyListeningService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            status.text = "Listening — speak a command directly…"
        } catch (_: Exception) {
            status.text = "Could not start. Open Buddy and try again."
        }
    }
}
