package com.example.buddy

/**
 * Central, explicit allowlist for actions the agent may propose.
 *
 * This registry is deliberately platform-independent so it can be unit-tested.
 * Adding a tool here does not automatically grant Android permissions or bypass
 * the per-step confirmation performed by BuddyListeningService.
 */
object AgentToolRegistry {
    data class ToolDefinition(
        val name: String,
        val description: String,
        val requiresUserConfirmation: Boolean = true,
        val maxValueLength: Int = 180
    )

    private val definitions = listOf(
        ToolDefinition("OPEN_APP", "Open an installed app by its visible name."),
        ToolDefinition("TYPE_TEXT", "Insert exact dictated text into the field the user has focused.", maxValueLength = 180),
        ToolDefinition("WEB_SEARCH", "Open a web search for the user's query.")
    )

    private val byName = definitions.associateBy { it.name }

    fun find(name: String): ToolDefinition? = byName[name.trim().uppercase()]

    fun supports(name: String): Boolean = find(name) != null

    fun all(): List<ToolDefinition> = definitions.toList()

    /**
     * Validates an AI-proposed tool call before it can enter the execution queue.
     * Unknown tools, empty values, oversized values and control characters fail closed.
     */
    fun validate(name: String, value: String): Boolean {
        val definition = find(name) ?: return false
        return value.isNotBlank() &&
            value.length <= definition.maxValueLength &&
            value.none { it.isISOControl() }
    }
}
