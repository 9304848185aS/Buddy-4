package com.example.buddy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

class BuddyListeningService : Service(), RecognitionListener, TextToSpeech.OnInitListener {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ready = false
    private var restarting = false
    private var stopping = false
    private var pauseListening = false
    private var pendingAction: AiBrain.ActionPlan? = null
    private val actionTimeoutHandler = Handler(Looper.getMainLooper())
    private var actionTimeout: Runnable? = null

    private fun clearPendingAction() {
        actionTimeout?.let { actionTimeoutHandler.removeCallbacks(it) }
        actionTimeout = null
        pendingAction = null
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, "buddy_listening").setContentTitle("Buddy is listening")
                .setContentText("Voice control is active. Say stop to end listening.")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now).build()
        else Notification.Builder(this).setContentTitle("Buddy is listening")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).build()
        startForeground(101, notification)
        tts = TextToSpeech(this, this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
            listen()
        } else stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("buddy_listening", "Buddy listening", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale("hi", "IN")
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { }
                override fun onDone(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).post {
                        if (!stopping) {
                            pauseListening = false
                            listen()
                        }
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).post {
                        pauseListening = false
                        if (!stopping) listen()
                    }
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    Handler(Looper.getMainLooper()).post {
                        pauseListening = false
                        if (!stopping) listen()
                    }
                }
            })
        }
    }

    private fun listen() {
        if (restarting || stopping || pauseListening || recognizer == null) return
        restarting = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try { recognizer?.startListening(intent) } catch (_: Exception) { restarting = false; stopSelf() }
    }

    private fun handle(text: String) {
        val raw = text.lowercase(Locale.ROOT).trim()
        if (raw.isBlank() || stopping) return
        if (raw == "buddy" || raw == "बडी" || raw == "बड्डी") {
            speak("जी, बताइए।")
            return
        }
        // Accept the requested single-word wake name at the start of a command.
        val s = raw.replace(Regex("^(buddy|बडी|बड्डी)[, ]*"), "").trim()
        if (s.isBlank()) return
        val waiting = pendingAction
        if (waiting != null) {
            when {
                s.contains("नहीं") || s.contains("मत करो") || s == "no" || s == "cancel" -> {
                    clearPendingAction()
                    speak("ठीक है, action रद्द कर दिया।")
                }
                s == "हाँ" || s == "हां" || s.contains("कर दो") ||
                    s == "yes" || s == "confirm" || s == "haan" -> executePendingAction()
                else -> speak("इस action के लिए हाँ या नहीं बोलिए।")
            }
            return
        }
        when {
            s.contains("रुक") || s.contains("बंद सुनना") || s.contains("stop listening") || s == "stop" -> {
                stopping = true
                speak("ठीक है, सुनना बंद कर रहा हूँ")
                Handler(Looper.getMainLooper()).postDelayed({ stopSelf() }, 1500)
            }
            s.contains("समय") || s.contains("time") || s.contains("कितने बजे") -> {
                val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                speak("अभी समय है $now")
            }
            s.contains("तारीख") || s.contains("date") -> {
                val today = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("hi", "IN")).format(Date())
                speak("आज $today है")
            }
            s.contains("बैटरी") || s.contains("battery") -> {
                val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                speak("बैटरी " + bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + " प्रतिशत है")
            }
            s.startsWith("type ") || s.startsWith("टाइप करो ") ||
                s.startsWith("लिखो ") || s.startsWith("लिख दो ") -> typeIntoFocusedField(s)
            s.contains("अलार्म") || s.contains("alarm") -> setAlarm(s)
            s.contains("वाईफाई") || s.contains("wi-fi") || s.contains("wifi") -> openSettings(Settings.Panel.ACTION_WIFI, "वाई-फाई सेटिंग")
            s.contains("ब्लूटूथ") || s.contains("bluetooth") -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "ब्लूटूथ सेटिंग")
            s.contains("सेटिंग") || s.contains("settings") -> openSettings(Settings.ACTION_SETTINGS, "सेटिंग्स")
            s.contains("कैमरा") || s.contains("camera") -> openIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "कैमरा")
            s.contains("कॉल") || s.contains("फोन") || s.contains("dialer") -> openIntent(Intent(Intent.ACTION_DIAL), "फोन डायलर")
            s.contains("मैसेज") || s.contains("message") || s.contains("sms") -> openIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING), "मैसेज")
            s.contains("कॉन्टैक्ट") || s.contains("contacts") -> openIntent(Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people/")), "कॉन्टैक्ट")
            s.contains("मैप") || s.contains("maps") || s.contains("नेविगेशन") -> openIntent(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")), "मैप")
            s.contains("यूट्यूब") || s.contains("youtube") -> openApp("com.google.android.youtube", "YouTube")
            s.contains("क्रोम") || s.contains("chrome") || s.contains("ब्राउज़र") -> openApp("com.android.chrome", "Chrome")
            s.contains("जीमेल") || s.contains("gmail") -> openApp("com.google.android.gm", "Gmail")
            s.contains("प्ले स्टोर") || s.contains("play store") -> openApp("com.android.vending", "Play Store")
            s.contains("फोटो") || s.contains("गैलरी") || s.contains("gallery") || s.contains("photos") -> openApp("com.google.android.apps.photos", "Photos")
            s.contains("कैलकुलेटर") || s.contains("calculator") -> openIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR), "कैलकुलेटर")
            s.startsWith("search ") || s.startsWith("सर्च ") -> searchWeb(s)
            else -> openNamedAppOrReply(s)
        }
    }

    private fun typeIntoFocusedField(command: String) {
        val normalized = command.lowercase(Locale.ROOT)
        val text = when {
            normalized.startsWith("type ") -> command.substringAfter(" ").trim()
            normalized.startsWith("टाइप करो ") -> command.substring("टाइप करो ".length).trim()
            normalized.startsWith("लिख दो ") -> command.substring("लिख दो ".length).trim()
            else -> command.substring("लिखो ".length).trim()
        }
        if (text.isBlank()) {
            speak("जो लिखवाना है, वह बोलिए।")
            return
        }
        if (text.length > 1000) {
            speak("एक बार में अधिकतम एक हजार अक्षर लिख सकता हूँ।")
            return
        }
        pauseListening = true
        BuddyTypingAccessibilityService.typeIntoFocusedField(text) { success ->
            speak(if (success) "आपका text चुने हुए box में डाल दिया है। भेजने या search करने से पहले जाँच लें।"
                  else "पहले ऐप में text box पर tap करके cursor रखें और Buddy की Accessibility typing permission चालू करें।")
        }
    }

    private fun searchWeb(command: String) {
        val query = command.removePrefix("search").removePrefix("सर्च").trim()
        if (query.isBlank()) { speak("क्या search करना है?"); return }
        val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(packageManager) != null) openIntent(intent, "सर्च")
        else openIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))), "सर्च")
    }

    private fun launchNamedApp(command: String): Boolean {
        val normalized = command.lowercase(Locale.ROOT)
            .replace("please", "")
            .replace("open", "")
            .replace("launch", "")
            .replace("खोलकर", "")
            .replace("खोलना", "")
            .replace("खोलो", "")
            .replace("खोल", "")
            .replace("चालू करो", "")
            .replace("चालू कर", "")
            .replace("ऐप", "")
            .replace("app", "")
            .replace("buddy", "")
            .trim()
            .replace(Regex("\\s+"), " ")

        if (normalized.isBlank()) return false
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = packageManager.queryIntentActivities(launcher, 0)
        val match = matches.firstOrNull { info ->
            info.loadLabel(packageManager).toString().trim().equals(normalized, ignoreCase = true)
        } ?: matches.firstOrNull { info ->
            val label = info.loadLabel(packageManager).toString().lowercase(Locale.ROOT)
            label.contains(normalized) || normalized.contains(label)
        } ?: return false

        val launch = packageManager.getLaunchIntentForPackage(match.activityInfo.packageName) ?: return false
        return try {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
            speak(infoLabel(match.activityInfo.packageName))
            true
        } catch (_: Exception) {
            speak("यह ऐप अभी नहीं खुल पाया")
            true
        }
    }

    private fun openNamedAppOrReply(command: String) {
        if (launchNamedApp(command)) return
        when {
            command.contains("नमस्ते") || command.contains("hello") || command == "hi" -> speak("नमस्ते, बताइए मैं क्या मदद करूँ?")
            command.contains("तुम कौन") || command.contains("who are you") -> speak("मैं Buddy हूँ, आपका फोन voice assistant।")
            command.contains("धन्यवाद") || command.contains("thank you") -> speak("आपका स्वागत है")
            else -> requestAiPlannedAction(command)
        }
    }

    private fun requestAiPlannedAction(command: String) {
        pauseListening = true
        AiBrain.planAction(this, command) { plan ->
            if (plan.action == "NONE") {
                AiBrain.ask(this, command) { answer -> speak(answer) }
                return@planAction
            }
            clearPendingAction()
            pendingAction = plan
            val timeout = Runnable {
                if (pendingAction === plan) {
                    pendingAction = null
                    actionTimeout = null
                    speak("समय पूरा हुआ। सुरक्षा के लिए action रद्द कर दिया।")
                }
            }
            actionTimeout = timeout
            actionTimeoutHandler.postDelayed(timeout, 15000L)
            val description = when (plan.action) {
                "OPEN_APP" -> "${plan.value} ऐप खोलना"
                "TYPE_TEXT" -> "चुने हुए text box में ${plan.value} लिखना"
                "WEB_SEARCH" -> "${plan.value} को वेब पर search करना"
                else -> "यह काम करना"
            }
            speak("मैं $description चाहता हूँ। करने के लिए हाँ बोलिए, रद्द करने के लिए नहीं।")
        }
    }

    private fun executePendingAction() {
        val plan = pendingAction ?: return
        clearPendingAction()
        when (plan.action) {
            "OPEN_APP" -> if (!launchNamedApp(plan.value)) speak("${plan.value} ऐप नहीं मिला।")
            "TYPE_TEXT" -> typeIntoFocusedField("type ${plan.value}")
            "WEB_SEARCH" -> searchWeb("search ${plan.value}")
            else -> speak("यह action उपलब्ध नहीं है।")
        }
    }

    private fun infoLabel(packageName: String): String {
        val label = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) { "ऐप" }
        return "$label खोल रहा हूँ"
    }

    private fun setAlarm(command: String) {
        val match = Regex("(\\d{1,2})(?::(\\d{2}))?").find(command)
        if (match == null) { speak("कृपया समय बोलिए, जैसे अलार्म सात तीस"); return }
        val hourInput = match.groupValues[1].toIntOrNull() ?: return
        val minute = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        if (hourInput !in 0..23 || minute !in 0..59) { speak("समय सही नहीं है"); return }
        val hour = if (command.contains("pm") && hourInput in 1..11) hourInput + 12 else if (command.contains("am") && hourInput == 12) 0 else hourInput
        val alarm = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Buddy alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        openIntent(alarm, "अलार्म")
    }

    private fun openSettings(action: String, label: String) {
        openIntent(Intent(action), label)
    }

    private fun openIntent(intent: Intent, label: String) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                speak("$label खोल रहा हूँ")
            } else speak("$label इस फोन में उपलब्ध नहीं है")
        } catch (_: Exception) { speak("$label नहीं खुल पाया") }
    }

    private fun openApp(pkg: String, label: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
            speak("$label खोल रहा हूँ")
        } else speak("$label इस फोन में नहीं मिला")
    }

    private fun speak(message: String) {
        if (ready) {
            pauseListening = true
            val result = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "buddy_reply")
            if (result == TextToSpeech.ERROR) {
                pauseListening = false
                if (!stopping) listen()
            }
        }
    }

    override fun onReadyForSpeech(params: Bundle?) { restarting = false }
    override fun onBeginningOfSpeech() { }
    override fun onRmsChanged(rmsdB: Float) { }
    override fun onBufferReceived(buffer: ByteArray?) { }
    override fun onEndOfSpeech() { }
    override fun onError(error: Int) {
        restarting = false
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) stopSelf()
        else if (error != SpeechRecognizer.ERROR_CLIENT && !stopping && !pauseListening) {
            Handler(Looper.getMainLooper()).postDelayed({ listen() }, 500)
        }
    }
    override fun onResults(results: Bundle?) {
        restarting = false
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { handle(it) }
        if (!stopping && !pauseListening) listen()
    }
    override fun onPartialResults(partialResults: Bundle?) { }
    override fun onEvent(eventType: Int, params: Bundle?) { }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
    override fun onDestroy() {
        recognizer?.destroy(); recognizer = null
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
