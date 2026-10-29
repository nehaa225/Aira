package com.aira.accessibility.agent

import android.util.Log

class AiraAgent(
    private val toolRegistry: AiraToolRegistry
) {

    companion object {
        private const val TAG = "AIRA_AGENT"
    }

    suspend fun processRequest(
        userRequest: String
    ): AiraToolResult {

        val request = userRequest.trim()

        if (request.isEmpty()) {
            return AiraToolResult(
                success = false,
                message = "I didn't receive a request."
            )
        }

        Log.d(TAG, "User request: $request")

        val plan = createPlan(request)

        Log.d(TAG, "Selected tool: ${plan.toolName}")
        Log.d(TAG, "Parameters: ${plan.parameters}")

        val tool = toolRegistry.getTool(plan.toolName)

        if (tool == null) {
            return AiraToolResult(
                success = false,
                message = "I don't currently have that capability."
            )
        }

        return try {

            tool.execute(plan.parameters)

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Tool execution failed",
                e
            )

            AiraToolResult(
                success = false,
                message = "I couldn't complete that action."
            )
        }
    }

    private fun createPlan(
        request: String
    ): AiraActionPlan {

        val normalized = request
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

        // =========================================
        // WHATSAPP MESSAGE
        // =========================================

        if (
            normalized.contains("whatsapp") &&
            (
                    normalized.contains("send") ||
                            normalized.contains("message") ||
                            normalized.contains("text")
                    )
        ) {

            val parsed = parseWhatsAppMessage(request)

            Log.d(
                TAG,
                "WhatsApp contact = ${parsed.first}"
            )

            Log.d(
                TAG,
                "WhatsApp message = ${parsed.second}"
            )

            if (
                parsed.first.isNotBlank() &&
                parsed.second.isNotBlank()
            ) {

                return AiraActionPlan(
                    toolName = "send_whatsapp_message",
                    parameters = mapOf(
                        "contact" to parsed.first,
                        "message" to parsed.second
                    )
                )
            }
        }

        // =========================================
        // BACK
        // =========================================

        if (
            normalized == "back" ||
            normalized.contains("go back") ||
            normalized.contains("press back")
        ) {

            return AiraActionPlan(
                toolName = "go_back"
            )
        }

        // =========================================
        // HOME
        // =========================================


        if (
            normalized == "home" ||
            normalized == "go home" ||
            normalized == "go to home" ||
            normalized == "take me home" ||
            normalized == "return home" ||
            normalized == "return to home" ||
            normalized.contains("go home") ||
            normalized.contains("go to home") ||
            normalized.contains("home screen")
        ) {
            return AiraActionPlan(
                toolName = "go_home"
            )
        }


        // =========================================
        // READ SCREEN
        // =========================================

        if (
            normalized.contains("read my screen") ||
            normalized.contains("read the screen") ||
            normalized.contains("what is on my screen") ||
            normalized.contains("what's on my screen") ||
            normalized.contains("read screen")
        ) {

            return AiraActionPlan(
                toolName = "read_screen"
            )
        }

        // =========================================
        // NOTIFICATIONS
        // =========================================

        if (
            normalized.contains("open notifications") ||
            normalized.contains("show notifications") ||
            normalized.contains("read notifications")
        ) {

            return AiraActionPlan(
                toolName = "system_action",
                parameters = mapOf(
                    "action" to "notifications"
                )
            )
        }

        // =========================================
        // QUICK SETTINGS
        // =========================================

        if (
            normalized.contains("quick settings")
        ) {

            return AiraActionPlan(
                toolName = "system_action",
                parameters = mapOf(
                    "action" to "quick_settings"
                )
            )
        }

        // =========================================
        // RECENT APPS
        // =========================================

        if (
            normalized.contains("recent apps") ||
            normalized.contains("open recents")
        ) {

            return AiraActionPlan(
                toolName = "system_action",
                parameters = mapOf(
                    "action" to "recents"
                )
            )
        }

        // =========================================
        // OPEN APP
        // =========================================

        if (
            normalized.startsWith("open ") ||
            normalized.startsWith("launch ")
        ) {

            val appName = request
                .substringAfter(" ")
                .trim()

            return AiraActionPlan(
                toolName = "open_app",
                parameters = mapOf(
                    "app" to appName
                )
            )
        }

        // =========================================
        // FIND
        // =========================================

        if (
            normalized.startsWith("find ") ||
            normalized.startsWith("locate ") ||
            normalized.startsWith("where is ")
        ) {

            val target = when {

                normalized.startsWith("find ") ->
                    request.substringAfter("find ")

                normalized.startsWith("locate ") ->
                    request.substringAfter("locate ")

                else ->
                    request.substringAfter("where is ")
            }.trim()

            return AiraActionPlan(
                toolName = "find_element",
                parameters = mapOf(
                    "description" to target
                )
            )
        }

        // =========================================
        // CLICK / TAP
        // =========================================

        if (
            normalized.startsWith("click ") ||
            normalized.startsWith("tap ") ||
            normalized.startsWith("press ") ||
            normalized.startsWith("select ")
        ) {

            val target = when {

                normalized.startsWith("click ") ->
                    request.substringAfter("click ")

                normalized.startsWith("tap ") ->
                    request.substringAfter("tap ")

                normalized.startsWith("press ") ->
                    request.substringAfter("press ")

                else ->
                    request.substringAfter("select ")
            }.trim()

            return AiraActionPlan(
                toolName = "click_element",
                parameters = mapOf(
                    "description" to target
                )
            )
        }

        // =========================================
        // SCROLL DOWN
        // =========================================

        if (
            normalized.contains("scroll down") ||
            normalized.contains("swipe down")
        ) {

            return AiraActionPlan(
                toolName = "scroll",
                parameters = mapOf(
                    "direction" to "down"
                )
            )
        }

        // =========================================
        // SCROLL UP
        // =========================================

        if (
            normalized.contains("scroll up") ||
            normalized.contains("swipe up")
        ) {

            return AiraActionPlan(
                toolName = "scroll",
                parameters = mapOf(
                    "direction" to "up"
                )
            )
        }

        // =========================================
        // TYPE
        // =========================================

        if (
            normalized.startsWith("type ")
        ) {

            val text = request
                .substringAfter("type ")
                .trim()

            return AiraActionPlan(
                toolName = "type_text",
                parameters = mapOf(
                    "text" to text
                )
            )
        }

        // =========================================
        // UNKNOWN
        // =========================================

        return AiraActionPlan(
            toolName = "unknown"
        )
    }

    // =========================================================
    // PARSE WHATSAPP MESSAGE
    // =========================================================

    private fun parseWhatsAppMessage(
        request: String
    ): Pair<String, String> {

        var text = request
            .trim()
            .replace(Regex("\\s+"), " ")

        val lower = text.lowercase()

        Log.d(
            TAG,
            "Parsing WhatsApp request: $text"
        )

        // -----------------------------------------
        // Remove wake word if it reached the agent
        // -----------------------------------------

        text = text.replace(
            Regex(
                "^hey\\s+(jarvis|aira)[,\\s]*",
                RegexOption.IGNORE_CASE
            ),
            ""
        ).trim()

        // -----------------------------------------
        // Remove common WhatsApp prefixes
        // -----------------------------------------

        text = text.replace(
            Regex(
                "^send\\s+(a\\s+)?whatsapp\\s+(message|text)\\s*",
                RegexOption.IGNORE_CASE
            ),
            ""
        ).trim()

        text = text.replace(
            Regex(
                "^whatsapp\\s+(message|text)\\s*",
                RegexOption.IGNORE_CASE
            ),
            ""
        ).trim()

        val cleanedLower = text.lowercase()

        // -----------------------------------------
        // Pattern:
        //
        // to Rahul saying hello bro
        //
        // -----------------------------------------

        val sayingRegex = Regex(
            "^to\\s+(.+?)\\s+saying\\s+(.+)$",
            RegexOption.IGNORE_CASE
        )

        val sayingMatch = sayingRegex.find(text)

        if (sayingMatch != null) {

            val contact = sayingMatch
                .groupValues[1]
                .trim()

            val message = sayingMatch
                .groupValues[2]
                .trim()

            return Pair(
                contact,
                message
            )
        }

        // -----------------------------------------
        // Pattern:
        //
        // to Rahul that hello bro
        //
        // -----------------------------------------

        val thatRegex = Regex(
            "^to\\s+(.+?)\\s+that\\s+(.+)$",
            RegexOption.IGNORE_CASE
        )

        val thatMatch = thatRegex.find(text)

        if (thatMatch != null) {

            val contact = thatMatch
                .groupValues[1]
                .trim()

            val message = thatMatch
                .groupValues[2]
                .trim()

            return Pair(
                contact,
                message
            )
        }

        // -----------------------------------------
        // Pattern:
        //
        // to Rahul: hello bro
        //
        // -----------------------------------------

        val colonRegex = Regex(
            "^to\\s+(.+?):\\s*(.+)$",
            RegexOption.IGNORE_CASE
        )

        val colonMatch = colonRegex.find(text)

        if (colonMatch != null) {

            val contact = colonMatch
                .groupValues[1]
                .trim()

            val message = colonMatch
                .groupValues[2]
                .trim()

            return Pair(
                contact,
                message
            )
        }

        // -----------------------------------------
        // Pattern:
        //
        // Rahul saying hello bro
        //
        // -----------------------------------------

        val directSayingRegex = Regex(
            "^(.+?)\\s+saying\\s+(.+)$",
            RegexOption.IGNORE_CASE
        )

        val directSayingMatch =
            directSayingRegex.find(text)

        if (directSayingMatch != null) {

            val contact = directSayingMatch
                .groupValues[1]
                .trim()

            val message = directSayingMatch
                .groupValues[2]
                .trim()

            return Pair(
                contact,
                message
            )
        }

        // -----------------------------------------
        // Pattern:
        //
        // Rahul that hello bro
        //
        // -----------------------------------------

        val directThatRegex = Regex(
            "^(.+?)\\s+that\\s+(.+)$",
            RegexOption.IGNORE_CASE
        )

        val directThatMatch =
            directThatRegex.find(text)

        if (directThatMatch != null) {

            val contact = directThatMatch
                .groupValues[1]
                .trim()

            val message = directThatMatch
                .groupValues[2]
                .trim()

            return Pair(
                contact,
                message
            )
        }

        // -----------------------------------------
        // Original "to" fallback
        // -----------------------------------------

        val toRegex = Regex(
            "^to\\s+(.+)$",
            RegexOption.IGNORE_CASE
        )

        val toMatch = toRegex.find(text)

        if (toMatch != null) {

            val remainder = toMatch
                .groupValues[1]
                .trim()

            val remainderLower =
                remainder.lowercase()

            val sayingPosition =
                remainderLower.indexOf(" saying ")

            if (sayingPosition != -1) {

                val contact =
                    remainder.substring(
                        0,
                        sayingPosition
                    ).trim()

                val message =
                    remainder.substring(
                        sayingPosition + 8
                    ).trim()

                return Pair(
                    contact,
                    message
                )
            }
        }

        Log.d(
            TAG,
            "Could not parse WhatsApp message"
        )

        return Pair("", "")
    }
}

data class AiraActionPlan(
    val toolName: String,
    val parameters: Map<String, String> = emptyMap()
)