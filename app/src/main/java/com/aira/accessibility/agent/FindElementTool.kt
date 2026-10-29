package com.aira.accessibility.agent

import com.aira.accessibility.AiraAccessibilityService

class FindElementTool(
    private val service:
    AiraAccessibilityService
) : AiraTool {

    override val name =
        "find_element"

    override val description =
        "Find a visible UI element by its text or description."

    override suspend fun execute(
        parameters: Map<String, String>
    ): AiraToolResult {

        val description =
            parameters["description"]
                ?.trim()

        if (description.isNullOrEmpty()) {

            return AiraToolResult(
                false,
                "Please specify the element to find."
            )
        }

        val node =
            service.findElement(
                description
            )

        return if (node != null) {

            AiraToolResult(
                true,
                "I found $description."
            )

        } else {

            AiraToolResult(
                false,
                "I couldn't find $description."
            )
        }
    }
}