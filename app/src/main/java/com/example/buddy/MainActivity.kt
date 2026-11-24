package com.example.buddy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        val title = TextView(this).apply { text = "BUDDY"; textSize = 34f; gravity = Gravity.CENTER }
        status = TextView(this).apply { text = "Your voice assistant is ready"; textSize = 17f; gravity = Gravity.CENTER }
        val start = Button(this).apply { text = "START LISTENING"; setOnClickListener { requestAndStart() } }
        val stop = Button(this).apply { text = "STOP"; setOnClickListener { stopService(Intent(this@MainActivity, BuddyListeningService::class.java)); status.text = "Stopped" } }
        layout.addView(title); layout.addView(status); layout.addView(start); layout.addView(stop)
        setContentView(layout)
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
            status.text = "Listening for “Buddy”…"
        } catch (e: Exception) {
            status.text = "Could not start. Open Buddy and try again."
        }
    }
}
