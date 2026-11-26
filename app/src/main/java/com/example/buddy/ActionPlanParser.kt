package com.example.buddy

import org.json.JSONObject
import java.util.Locale

/**
 * Strict allowlist parser for AI-proposed phone actions.
 * Rejects unexpected fields and malformed values before execution.
 */
object ActionPlanParser {
    fun parse(raw: String): List<AiBrain.ActionStep> {
        return try {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start < 0 || end <= start) return emptyList()
            val root = JSONObject(raw.substring(start, end + 1))
            if (root.keys().asSequence().toSet() != setOf("steps", "reason")) return emptyList()

            val steps = root.optJSONArray("steps") ?: return emptyList()
            if (steps.length() !in 1..3) return emptyList()

            val result = mutableListOf<AiBrain.ActionStep>()
            for (index in 0 until steps.length()) {
                val item = steps.optJSONObject(index) ?: return emptyList()
                if (item.keys().asSequence().toSet() != setOf("action", "value")) return emptyList()

                val action = item.optString("action").uppercase(Locale.ROOT)
                val value = item.optString("value").trim()
                if (action !in ALLOWED_ACTIONS || value.isBlank() || value.length > 180 ||
                    value.any { it.isISOControl() }) {
                    return emptyList()
                }
                result.add(AiBrain.ActionStep(action, value))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private val ALLOWED_ACTIONS = setOf("OPEN_APP", "TYPE_TEXT", "WEB_SEARCH")
}
