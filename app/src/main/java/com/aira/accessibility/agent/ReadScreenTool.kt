package com.aira.accessibility.agent

import com.aira.accessibility.AiraAccessibilityService

class ReadScreenTool(
    private val accessibilityService: AiraAccessibilityService
) : AiraTool {

    override val name: String =
        "read_screen"

    override val description: String =
        "Read and describe the text and accessible content currently visible on the screen."

    override suspend fun execute(
        parameters: Map<String, String>
    ): AiraToolResult {

        val screenText =
            accessibilityService.getCurrentScreenText()

        if (screenText.isBlank()) {

            return AiraToolResult(
                success = false,
                message =
                    "I couldn't find readable content on the current screen."
            )
        }

        return AiraToolResult(
            success = true,
            message = screenText
        )
    }
}