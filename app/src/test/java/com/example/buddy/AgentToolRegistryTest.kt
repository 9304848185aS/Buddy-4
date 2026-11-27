package com.example.buddy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentToolRegistryTest {
    @Test fun exposesOnlyExplicitlyRegisteredTools() {
        assertTrue(AgentToolRegistry.supports("OPEN_APP"))
        assertTrue(AgentToolRegistry.supports("type_text"))
        assertTrue(AgentToolRegistry.supports("WEB_SEARCH"))
        assertFalse(AgentToolRegistry.supports("SEND_MESSAGE"))
        assertFalse(AgentToolRegistry.supports("PAYMENT"))
        assertFalse(AgentToolRegistry.supports("ARBITRARY_TAP"))
    }

    @Test fun validatesValuesBeforeQueueing() {
        assertTrue(AgentToolRegistry.validate("OPEN_APP", "Chrome"))
        assertTrue(AgentToolRegistry.validate("WEB_SEARCH", "weather in Pune"))
        assertFalse(AgentToolRegistry.validate("WEB_SEARCH", "  "))
        assertFalse(AgentToolRegistry.validate("WEB_SEARCH", "bad\nvalue"))
        assertFalse(AgentToolRegistry.validate("UNKNOWN_TOOL", "value"))
        assertFalse(AgentToolRegistry.validate("TYPE_TEXT", "x".repeat(181)))
    }

    @Test fun everyRegisteredToolRequiresConfirmationByDefault() {
        assertNotNull(AgentToolRegistry.find("OPEN_APP"))
        assertTrue(AgentToolRegistry.all().isNotEmpty())
        assertTrue(AgentToolRegistry.all().all { it.requiresUserConfirmation })
    }
}
