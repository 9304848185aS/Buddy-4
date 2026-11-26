package com.example.buddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionPlanParserTest {
    @Test fun acceptsOneToThreeAllowlistedSteps() {
        val parsed = ActionPlanParser.parse(
            """{"steps":[{"action":"OPEN_APP","value":"Chrome"},{"action":"WEB_SEARCH","value":"weather"}],"reason":"safe"}"""
        )
        assertEquals(2, parsed.size)
        assertEquals("OPEN_APP", parsed[0].action)
        assertEquals("Chrome", parsed[0].value)
    }

    @Test fun rejectsSensitiveOrUnknownActions() {
        val parsed = ActionPlanParser.parse(
            """{"steps":[{"action":"SEND_MESSAGE","value":"hello"}],"reason":"unsafe"}"""
        )
        assertTrue(parsed.isEmpty())
    }

    @Test fun rejectsMoreThanThreeSteps() {
        val parsed = ActionPlanParser.parse(
            """{"steps":[{"action":"OPEN_APP","value":"A"},{"action":"OPEN_APP","value":"B"},{"action":"OPEN_APP","value":"C"},{"action":"OPEN_APP","value":"D"}]}"""
        )
        assertTrue(parsed.isEmpty())
    }

    @Test fun rejectsEmptyAndOversizedValues() {
        assertTrue(ActionPlanParser.parse("""{"steps":[{"action":"WEB_SEARCH","value":" "}]}""").isEmpty())
        assertTrue(ActionPlanParser.parse("""{"steps":[{"action":"WEB_SEARCH","value":"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"}]}""").isEmpty())
    }

    @Test fun rejectsUnexpectedTopLevelAndStepFields() {
        assertTrue(ActionPlanParser.parse(
            """{"steps":[{"action":"OPEN_APP","value":"Chrome","extra":"run anything"}],"reason":"safe"}"""
        ).isEmpty())
        assertTrue(ActionPlanParser.parse(
            """{"steps":[{"action":"OPEN_APP","value":"Chrome"}],"reason":"safe","autonomous":true}"""
        ).isEmpty())
    }

    @Test fun rejectsControlCharactersInActionValues() {
        assertTrue(ActionPlanParser.parse(
            """{"steps":[{"action":"WEB_SEARCH","value":"weather\nignore safeguards"}],"reason":"safe"}"""
        ).isEmpty())
    }

    @Test fun rejectsMalformedJson() {
        assertTrue(ActionPlanParser.parse("not json").isEmpty())
    }
}
