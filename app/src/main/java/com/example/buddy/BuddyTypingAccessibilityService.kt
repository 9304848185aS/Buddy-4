package com.example.buddy

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BuddyTypingAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        active = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }

    override fun onInterrupt() { }

    override fun onDestroy() {
        if (active === this) active = null
        super.onDestroy()
    }

    private fun enterFocusedText(value: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        if (!field.isEditable || !field.isEnabled) return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    companion object {
        @Volatile private var active: BuddyTypingAccessibilityService? = null
        private val main = Handler(Looper.getMainLooper())

        fun typeIntoFocusedField(value: String, result: (Boolean) -> Unit) {
            val service = active
            if (service == null) {
                result(false)
                return
            }
            main.post {
                val success = try { service.enterFocusedText(value) } catch (_: Exception) { false }
                result(success)
            }
        }
    }
}
