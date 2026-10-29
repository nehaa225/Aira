package com.aira.accessibility.agent

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.aira.accessibility.AiraAccessibilityService
import kotlinx.coroutines.delay

class SendWhatsAppMessageTool(
    private val accessibilityService: AiraAccessibilityService
) : AiraTool {

    override val name = "send_whatsapp_message"

    override val description =
        "Open WhatsApp, search for a contact, open the chat, type a message and send it."

    override suspend fun execute(
        parameters: Map<String, String>
    ): AiraToolResult {

        val contact = parameters["contact"]?.trim()
        val message = parameters["message"]?.trim()

        if (contact.isNullOrBlank()) {
            return AiraToolResult(
                false,
                "I need the contact name."
            )
        }

        if (message.isNullOrBlank()) {
            return AiraToolResult(
                false,
                "I need the message."
            )
        }

        try {

            // ==========================================
            // 1. OPEN WHATSAPP
            // ==========================================

            Log.d(
                "AIRA_WHATSAPP",
                "Opening WhatsApp"
            )

            val openResult =
                OpenAppTool(
                    accessibilityService.applicationContext
                ).execute(
                    mapOf("app" to "WhatsApp")
                )

            if (!openResult.success) {
                return AiraToolResult(
                    false,
                    "I couldn't open WhatsApp."
                )
            }

            delay(2000)

            // ==========================================
            // 2. CLICK SEARCH
            // ==========================================

            Log.d(
                "AIRA_WHATSAPP",
                "Clicking Search"
            )

            var searchClicked =
                accessibilityService.clickElement("Search")

            if (!searchClicked) {
                searchClicked =
                    accessibilityService.clickElement("search")
            }

            if (!searchClicked) {
                return AiraToolResult(
                    false,
                    "I couldn't find WhatsApp Search."
                )
            }

            delay(800)

            // ==========================================
            // 3. TYPE CONTACT
            // ==========================================

            Log.d(
                "AIRA_WHATSAPP",
                "Typing contact: $contact"
            )

            if (!accessibilityService.typeText(contact)) {
                return AiraToolResult(
                    false,
                    "I couldn't type the contact name."
                )
            }

            delay(1800)

            // ==========================================
            // 4. FIND CONTACT NODE
            // ==========================================

            Log.d(
                "AIRA_WHATSAPP",
                "Finding contact: $contact"
            )

            val contactNode =
                accessibilityService.findElement(contact)

            if (contactNode == null) {

                Log.d(
                    "AIRA_WHATSAPP",
                    "Contact node not found"
                )

                return AiraToolResult(
                    false,
                    "I couldn't find $contact in the search results."
                )
            }

            Log.d(
                "AIRA_WHATSAPP",
                "CONTACT NODE FOUND"
            )

            Log.d(
                "AIRA_WHATSAPP",
                "text=${contactNode.text}"
            )

            Log.d(
                "AIRA_WHATSAPP",
                "description=${contactNode.contentDescription}"
            )

            Log.d(
                "AIRA_WHATSAPP",
                "clickable=${contactNode.isClickable}"
            )

            Log.d(
                "AIRA_WHATSAPP",
                "focusable=${contactNode.isFocusable}"
            )

            // ==========================================
            // 5. CLICK CONTACT NODE DIRECTLY
            // ==========================================

            var chatOpened =
                clickNodeAndParents(contactNode)

            Log.d(
                "AIRA_WHATSAPP",
                "Contact click result = $chatOpened"
            )

            if (!chatOpened) {

                return AiraToolResult(
                    false,
                    "I found $contact but couldn't open the chat."
                )
            }

            // ==========================================
            // 6. WAIT FOR CHAT
            // ==========================================

            delay(2500)

            val screen =
                accessibilityService.getCurrentScreenText()

            Log.d(
                "AIRA_WHATSAPP",
                "SCREEN AFTER CONTACT CLICK:"
            )

            Log.d(
                "AIRA_WHATSAPP",
                screen
            )

            // ==========================================
            // 7. CHECK MESSAGE FIELD
            // ==========================================

            var messageField =
                accessibilityService.findElement(
                    "Type a message"
                )

            if (messageField == null) {

                messageField =
                    accessibilityService.findElement(
                        "Message"
                    )
            }

            if (messageField == null) {

                messageField =
                    accessibilityService.findElement(
                        "Enter message"
                    )
            }

            if (messageField == null) {

                return AiraToolResult(
                    false,
                    "The chat did not expose a message box."
                )
            }

            // ==========================================
            // 8. TYPE MESSAGE
            // ==========================================

            Log.d(
                "AIRA_WHATSAPP",
                "Typing message"
            )

            var messageTyped =
                accessibilityService.clickAndTypeIntoElement(
                    "Type a message",
                    message
                )

            if (!messageTyped) {

                messageTyped =
                    accessibilityService.clickAndTypeIntoElement(
                        "Message",
                        message
                    )
            }

            if (!messageTyped) {

                messageTyped =
                    accessibilityService.clickAndTypeIntoElement(
                        "Enter message",
                        message
                    )
            }

            if (!messageTyped) {

                return AiraToolResult(
                    false,
                    "I couldn't type the message."
                )
            }

            delay(500)

            // ==========================================
            // 9. SEND
            // ==========================================

            var sent =
                accessibilityService.clickElement("Send")

            if (!sent) {
                sent =
                    accessibilityService.clickElement("send")
            }

            if (!sent) {

                return AiraToolResult(
                    false,
                    "I typed the message but couldn't find Send."
                )
            }

            delay(700)

            return AiraToolResult(
                true,
                "Message sent to $contact."
            )

        } catch (e: Exception) {

            Log.e(
                "AIRA_WHATSAPP",
                "WhatsApp error",
                e
            )

            return AiraToolResult(
                false,
                "I couldn't send the WhatsApp message."
            )
        }
    }

    // =========================================================
    // CLICK NODE + CLICKABLE PARENTS
    // =========================================================

    private fun clickNodeAndParents(
        originalNode: AccessibilityNodeInfo
    ): Boolean {

        var node: AccessibilityNodeInfo? =
            originalNode

        var level = 0

        while (node != null && level < 8) {

            Log.d(
                "AIRA_WHATSAPP",
                "Trying click level=$level " +
                        "text=${node.text} " +
                        "description=${node.contentDescription} " +
                        "clickable=${node.isClickable}"
            )

            if (node.isClickable) {

                val clicked =
                    node.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )

                if (clicked) {

                    Log.d(
                        "AIRA_WHATSAPP",
                        "CLICK SUCCESS at level=$level"
                    )

                    return true
                }
            }

            node =
                node.parent

            level++
        }

        return false
    }
}