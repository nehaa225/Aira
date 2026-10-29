package com.aira.accessibility.agent

import com.aira.accessibility.AiraAccessibilityService

class TypeTextTool(
    private val service:
    AiraAccessibilityService
) : AiraTool {

    override val name =
        "type_text"

    override val description =
        "Type text into the currently focused editable field."

    override suspend fun execute(
        parameters: Map<String, String>
    ): AiraToolResult {

        val text =
            parameters["text"]

        if (text.isNullOrEmpty()) {

            return AiraToolResult(
                false,
                "There is no text to type."
            )
        }

        val success =
            service.typeText(text)

        return if (success) {

            AiraToolResult(
                true,
                "Typed the requested text."
            )

        } else {

            AiraToolResult(
                false,
                "I couldn't find an editable text field."
            )
        }
    }
}