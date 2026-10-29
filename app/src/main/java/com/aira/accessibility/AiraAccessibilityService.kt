package com.aira.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

import com.aira.accessibility.agent.AiraAgentManager

class AiraAccessibilityService : AccessibilityService() {

    companion object {

        private var instance: AiraAccessibilityService? = null

        fun getInstance(): AiraAccessibilityService? {
            return instance
        }

        private const val TAG = "AIRA_ACCESSIBILITY"
        private const val SCREEN_TAG = "AIRA_SCREEN"
        private const val DEBOUNCE_DELAY = 400L
    }

    private val handler =
        Handler(Looper.getMainLooper())

    private var lastScreenText = ""

    private lateinit var actionManager: AiraActionManager

    private lateinit var agentManager: AiraAgentManager

    private val screenReadRunnable = Runnable {
        readCurrentScreen()
    }

    override fun onServiceConnected() {

        super.onServiceConnected()

        instance = this

        actionManager =
            AiraActionManager(this)

        agentManager =
            AiraAgentManager(
                applicationContext,
                this
            )

        Log.d(
            TAG,
            "AIRA Accessibility Service connected"
        )

        Log.d(
            TAG,
            "AIRA Agent initialized"
        )

        Log.d(
            TAG,
            "Available tools:\n${agentManager.getAvailableTools()}"
        )
    }

    // =========================================================
    // ACCESSIBILITY EVENTS
    // =========================================================

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {

        if (event == null) {
            return
        }

        when (event.eventType) {

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {

                handler.removeCallbacks(
                    screenReadRunnable
                )

                handler.postDelayed(
                    screenReadRunnable,
                    DEBOUNCE_DELAY
                )
            }
        }
    }

    // =========================================================
    // READ CURRENT SCREEN
    // =========================================================

    private fun readCurrentScreen() {

        val screenText =
            getCurrentScreenText()

        if (screenText.isEmpty()) {
            return
        }

        if (screenText == lastScreenText) {
            return
        }

        lastScreenText =
            screenText

        Log.d(
            SCREEN_TAG,
            "SCREEN: $screenText"
        )
    }

    fun getCurrentScreenText(): String {

        val rootNode =
            rootInActiveWindow
                ?: return ""

        val screenText =
            StringBuilder()

        collectText(
            rootNode,
            screenText
        )

        return screenText
            .toString()
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )
    }

    private fun collectText(
        node: AccessibilityNodeInfo,
        output: StringBuilder
    ) {

        node.text
            ?.toString()
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
            ?.let {

                output.append(it)
                output.append(" | ")
            }

        node.contentDescription
            ?.toString()
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
            ?.let {

                output.append(it)
                output.append(" | ")
            }

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i)

            if (child != null) {

                collectText(
                    child,
                    output
                )
            }
        }
    }

    // =========================================================
    // FIND ELEMENT
    // =========================================================

    fun findElement(
        description: String
    ): AccessibilityNodeInfo? {

        val root =
            rootInActiveWindow
                ?: return null

        return findNodeRecursive(
            root,
            description.trim()
        )
    }

    private fun findNodeRecursive(
        node: AccessibilityNodeInfo,
        description: String
    ): AccessibilityNodeInfo? {

        val text =
            node.text
                ?.toString()
                ?.trim()
                ?: ""

        val contentDescription =
            node.contentDescription
                ?.toString()
                ?.trim()
                ?: ""

        if (
            text.contains(
                description,
                ignoreCase = true
            ) ||
            contentDescription.contains(
                description,
                ignoreCase = true
            )
        ) {

            return node
        }

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i)
                    ?: continue

            val result =
                findNodeRecursive(
                    child,
                    description
                )

            if (result != null) {
                return result
            }
        }

        return null
    }

    // =========================================================
    // CLICK ELEMENT
    // =========================================================

    fun clickElement(
        description: String
    ): Boolean {

        val node =
            findElement(description)
                ?: return false

        return clickNodeOrParent(node)
    }

    private fun clickNodeOrParent(
        node: AccessibilityNodeInfo
    ): Boolean {

        if (node.isClickable) {

            return node.performAction(
                AccessibilityNodeInfo.ACTION_CLICK
            )
        }

        var parent =
            node.parent

        while (parent != null) {

            if (parent.isClickable) {

                return parent.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }

            parent =
                parent.parent
        }

        return false
    }

    // =========================================================
    // ORIGINAL TYPE TEXT
    // =========================================================

    fun typeText(
        text: String
    ): Boolean {

        val root =
            rootInActiveWindow
                ?: return false

        val focused =
            root.findFocus(
                AccessibilityNodeInfo.FOCUS_INPUT
            )

        if (
            focused != null &&
            focused.isEditable
        ) {

            val arguments =
                Bundle()

            arguments.putCharSequence(
                AccessibilityNodeInfo
                    .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )

            return focused.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                arguments
            )
        }

        return findEditableNode(
            root,
            text
        )
    }

    private fun findEditableNode(
        node: AccessibilityNodeInfo,
        text: String
    ): Boolean {

        if (node.isEditable) {

            val arguments =
                Bundle()

            arguments.putCharSequence(
                AccessibilityNodeInfo
                    .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )

            return node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                arguments
            )
        }

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i)
                    ?: continue

            if (
                findEditableNode(
                    child,
                    text
                )
            ) {

                return true
            }
        }

        return false
    }

    // =========================================================
    // NEW:
    // CLICK + EXPLICITLY TYPE INTO TARGET FIELD
    // =========================================================

    fun clickAndTypeIntoElement(
        description: String,
        text: String
    ): Boolean {

        val root =
            rootInActiveWindow
                ?: return false

        val node =
            findNodeRecursive(
                root,
                description.trim()
            )
                ?: return false

        Log.d(
            TAG,
            "Target field found: $description"
        )

        // -----------------------------------------------------
        // Try to focus the exact target
        // -----------------------------------------------------

        if (node.isFocusable) {

            node.performAction(
                AccessibilityNodeInfo.ACTION_FOCUS
            )
        }

        // -----------------------------------------------------
        // Click target if possible
        // -----------------------------------------------------

        if (node.isClickable) {

            node.performAction(
                AccessibilityNodeInfo.ACTION_CLICK
            )
        }

        // -----------------------------------------------------
        // Try focus again after click
        // -----------------------------------------------------

        node.performAction(
            AccessibilityNodeInfo.ACTION_FOCUS
        )

        // -----------------------------------------------------
        // Directly set text on target if editable
        // -----------------------------------------------------

        if (node.isEditable) {

            val arguments =
                Bundle()

            arguments.putCharSequence(
                AccessibilityNodeInfo
                    .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )

            val result =
                node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    arguments
                )

            if (result) {

                Log.d(
                    TAG,
                    "Text successfully entered into $description"
                )

                return true
            }
        }

        // -----------------------------------------------------
        // Search editable children
        // -----------------------------------------------------

        val editableChild =
            findEditableChild(node)

        if (editableChild != null) {

            editableChild.performAction(
                AccessibilityNodeInfo.ACTION_FOCUS
            )

            val arguments =
                Bundle()

            arguments.putCharSequence(
                AccessibilityNodeInfo
                    .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )

            val result =
                editableChild.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    arguments
                )

            if (result) {

                Log.d(
                    TAG,
                    "Text successfully entered into child of $description"
                )

                return true
            }
        }

        // -----------------------------------------------------
        // Last fallback:
        // focus target and use normal typeText
        // -----------------------------------------------------

        node.performAction(
            AccessibilityNodeInfo.ACTION_FOCUS
        )

        val focusedRoot =
            rootInActiveWindow
                ?: return false

        val focusedNode =
            focusedRoot.findFocus(
                AccessibilityNodeInfo.FOCUS_INPUT
            )

        if (
            focusedNode != null &&
            focusedNode.isEditable
        ) {

            val arguments =
                Bundle()

            arguments.putCharSequence(
                AccessibilityNodeInfo
                    .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )

            return focusedNode.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                arguments
            )
        }

        return false
    }

    private fun findEditableChild(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        if (node.isEditable) {
            return node
        }

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i)
                    ?: continue

            val result =
                findEditableChild(child)

            if (result != null) {
                return result
            }
        }

        return null
    }

    // =========================================================
    // SCROLL
    // =========================================================

    fun scroll(
        direction: String
    ): Boolean {

        val root =
            rootInActiveWindow
                ?: return false

        val action =
            when (
                direction
                    .trim()
                    .lowercase()
            ) {

                "up",
                "back",
                "backward" ->
                    AccessibilityNodeInfo
                        .ACTION_SCROLL_BACKWARD

                "down",
                "forward" ->
                    AccessibilityNodeInfo
                        .ACTION_SCROLL_FORWARD

                else ->
                    AccessibilityNodeInfo
                        .ACTION_SCROLL_FORWARD
            }

        return scrollRecursive(
            root,
            action
        )
    }

    private fun scrollRecursive(
        node: AccessibilityNodeInfo,
        action: Int
    ): Boolean {

        if (node.isScrollable) {

            if (
                node.performAction(action)
            ) {

                return true
            }
        }

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i)
                    ?: continue

            if (
                scrollRecursive(
                    child,
                    action
                )
            ) {

                return true
            }
        }

        return false
    }

    // =========================================================
    // GLOBAL ACTIONS
    // =========================================================

    fun goBack(): Boolean {

        return performGlobalAction(
            GLOBAL_ACTION_BACK
        )
    }

    fun goHome(): Boolean {

        return performGlobalAction(
            GLOBAL_ACTION_HOME
        )
    }

    fun openNotifications(): Boolean {

        return performGlobalAction(
            GLOBAL_ACTION_NOTIFICATIONS
        )
    }

    fun openQuickSettings(): Boolean {

        return performGlobalAction(
            GLOBAL_ACTION_QUICK_SETTINGS
        )
    }

    fun openRecents(): Boolean {

        return performGlobalAction(
            GLOBAL_ACTION_RECENTS
        )
    }

    // =========================================================
    // AIRA AGENT
    // =========================================================

    fun processAiraRequest(
        request: String
    ): String {

        if (
            !::agentManager.isInitialized
        ) {

            return "AIRA is not ready yet."
        }

        return try {

            kotlinx.coroutines.runBlocking {

                agentManager
                    .processRequest(request)
                    .message
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Agent request failed",
                e
            )

            "I couldn't complete that request."
        }
    }

    // =========================================================
    // INTERRUPT
    // =========================================================

    override fun onInterrupt() {

        handler.removeCallbacks(
            screenReadRunnable
        )
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        instance = null

        handler.removeCallbacks(
            screenReadRunnable
        )

        super.onDestroy()
    }
}