package com.aira.accessibility.agent

import android.content.Context
import com.aira.accessibility.AiraAccessibilityService

class AiraAgentManager(
    private val context: Context,
    private val accessibilityService: AiraAccessibilityService
) {

    private val toolRegistry =
        AiraToolRegistry()

    private val agent =
        AiraAgent(toolRegistry)

    init {
        registerTools()
    }

    private fun registerTools() {

        toolRegistry.register(
            GoBackTool(accessibilityService)
        )

        toolRegistry.register(
            GoHomeTool(accessibilityService)
        )

        toolRegistry.register(
            OpenAppTool(context)
        )

        toolRegistry.register(
            ReadScreenTool(accessibilityService)
        )

        toolRegistry.register(
            FindElementTool(accessibilityService)
        )

        toolRegistry.register(
            ClickElementTool(accessibilityService)
        )

        toolRegistry.register(
            TypeTextTool(accessibilityService)
        )

        toolRegistry.register(
            ScrollTool(accessibilityService)
        )

        toolRegistry.register(
            SystemActionTool(accessibilityService)
        )
        toolRegistry.register(
            SendWhatsAppMessageTool(accessibilityService)
        )

    }

    suspend fun processRequest(
        request: String
    ): AiraToolResult {

        return agent.processRequest(request)
    }

    fun getAvailableTools(): String {

        return toolRegistry.getToolDescriptions()
    }
}