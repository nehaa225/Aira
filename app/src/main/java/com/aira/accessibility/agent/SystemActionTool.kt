package com.aira.accessibility.agent

import com.aira.accessibility.AiraAccessibilityService

class SystemActionTool(
    private val service:
    AiraAccessibilityService
) : AiraTool {

    override val name =
        "system_action"

    override val description =
        "Perform Android system actions such as home, back, notifications, quick settings, or recent apps."

    override suspend fun execute(
        parameters: Map<String, String>
    ): AiraToolResult {

        val action =
            parameters["action"]
                ?.lowercase()
                ?.trim()

        val success =
            when (action) {

                "back" ->
                    service.goBack()

                "home" ->
                    service.goHome()

                "notifications",
                "notification" ->
                    service.openNotifications()

                "quick_settings",
                "quick settings" ->
                    service.openQuickSettings()

                "recents",
                "recent apps" ->
                    service.openRecents()

                else ->
                    false
            }

        return if (success) {

            AiraToolResult(
                true,
                "System action completed."
            )

        } else {

            AiraToolResult(
                false,
                "I couldn't perform that system action."
            )
        }
    }
}