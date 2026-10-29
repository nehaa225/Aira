package com.aira.accessibility.agent

import com.aira.accessibility.AiraAccessibilityService

class ScrollTool(
    private val service:
    AiraAccessibilityService
) : AiraTool {

    override val name =
        "scroll"

    override val description =
        "Scroll the current screen up or down."

    override suspend fun execute(
        parameters: Map<String, String>
    ): AiraToolResult {

        val direction =
            parameters["direction"]
                ?.lowercase()
                ?: "down"

        val success =
            service.scroll(direction)

        return if (success) {

            AiraToolResult(
                true,
                "Scrolled $direction."
            )

        } else {

            AiraToolResult(
                false,
                "I couldn't scroll this screen."
            )
        }
    }
}