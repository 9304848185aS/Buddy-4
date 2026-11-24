package com.example.buddy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.AlarmClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class BuddyListeningService : Service(), RecognitionListener, TextToSpeech.OnInitListener {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ready = false
    private var awake = false
    private var restarting = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, "buddy_listening").setContentTitle("Buddy is listening")
                .setContentText("Say Buddy to activate, or tap Stop in the app.")
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
        if (ready) tts?.language = Locale("hi", "IN")
    }

    private fun listen() {
        if (restarting || recognizer == null) return
        restarting = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try { recognizer?.startListening(intent) } catch (_: Exception) { restarting = false; stopSelf() }
    }

    private fun handle(text: String) {
        val s = text.lowercase(Locale.ROOT).trim()
        if (!awake) {
            if (s.contains("buddy") || s.contains("बडी")) {
                awake = true
                speak("जी, बताइए")
            }
            return
        }
        when {
            s.contains("रुक") || s.contains("stop listening") || s == "stop" -> {
                speak("ठीक है, मैं रुक रहा हूँ")
                awake = false
            }
            s.contains("समय") || s.contains("time") -> {
                val now = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())
                speak("अभी समय है $now")
            }
            s.contains("अलार्म") || s.contains("alarm") -> setAlarm(s)
            s.contains("youtube") -> openApp("com.google.android.youtube", "YouTube")
            s.contains("chrome") -> openApp("com.android.chrome", "Chrome")
            s.contains("सेटिंग") || s.contains("settings") -> {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                speak("सेटिंग्स खोल रहा हूँ")
            }
            else -> speak("यह command अभी मेरे पास उपलब्ध नहीं है।")
        }
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
        if (alarm.resolveActivity(packageManager) != null) { startActivity(alarm); speak("अलार्म सेट करने के लिए स्क्रीन खोल रहा हूँ") }
        else speak("इस फोन में अलार्म ऐप नहीं मिला")
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
        if (ready) tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "buddy_reply")
    }

    override fun onReadyForSpeech(params: Bundle?) { restarting = false }
    override fun onBeginningOfSpeech() { }
    override fun onRmsChanged(rmsdB: Float) { }
    override fun onBufferReceived(buffer: ByteArray?) { }
    override fun onEndOfSpeech() { }
    override fun onError(error: Int) {
        restarting = false
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) stopSelf()
        else if (error != SpeechRecognizer.ERROR_CLIENT) listen()
    }
    override fun onResults(results: Bundle?) {
        restarting = false
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { handle(it) }
        listen()
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
