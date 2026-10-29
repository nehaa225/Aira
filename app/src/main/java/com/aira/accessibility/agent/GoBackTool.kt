package com.aira.accessibility.agent

import com.aira.accessibility.AiraAccessibilityService

class GoBackTool(
    private val accessibilityService: AiraAccessibilityService
) : AiraTool {

    override val name: String = "go_back"

    override val description: String =
        "Go back to the previous screen or page."

    override suspend fun execute(
        parameters: Map<String, String>
    ): AiraToolResult {

        val success =
            accessibilityService.goBack()

        return if (success) {

            AiraToolResult(
                success = true,
                message = "Went back successfully."
            )

        } else {

            AiraToolResult(
                success = false,
                message = "I could not go back."
            )
        }
    }
}