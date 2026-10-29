package com.aira.accessibility.agent

import com.aira.accessibility.AiraAccessibilityService

class GoHomeTool(
    private val accessibilityService: AiraAccessibilityService
) : AiraTool {

    override val name: String = "go_home"

    override val description: String =
        "Return to the Android home screen."

    override suspend fun execute(
        parameters: Map<String, String>
    ): AiraToolResult {

        val success =
            accessibilityService.goHome()

        return if (success) {

            AiraToolResult(
                success = true,
                message = "Returned to the home screen."
            )

        } else {

            AiraToolResult(
                success = false,
                message = "I could not return to the home screen."
            )
        }
    }
}