package com.aira.accessibility.agent

import com.aira.accessibility.AiraAccessibilityService

class ClickElementTool(
    private val service:
    AiraAccessibilityService
) : AiraTool {

    override val name =
        "click_element"

    override val description =
        "Click a visible UI element by its text or description."

    override suspend fun execute(
        parameters: Map<String, String>
    ): AiraToolResult {

        val description =
            parameters["description"]
                ?.trim()

        if (description.isNullOrEmpty()) {

            return AiraToolResult(
                false,
                "Please specify what I should click."
            )
        }

        val success =
            service.clickElement(
                description
            )

        return if (success) {

            AiraToolResult(
                true,
                "Clicked $description."
            )

        } else {

            AiraToolResult(
                false,
                "I couldn't click $description."
            )
        }
    }
}