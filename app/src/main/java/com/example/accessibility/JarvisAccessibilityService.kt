package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import com.example.tools.ToolExecutionResult
import java.util.Locale

class JarvisAccessibilityService :
    AccessibilityService() {

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        // Commands explicitly initiated by JARVIS
        // are handled through the public methods below.
    }

    override fun onInterrupt() {
    }

    override fun onServiceConnected() {

        super.onServiceConnected()

        instance = this
    }

    override fun onDestroy() {

        if (instance == this) {
            instance = null
        }

        super.onDestroy()
    }

    // ------------------------------------------------------------
    // GLOBAL ACTIONS
    // ------------------------------------------------------------

    fun performGoHome():
            ToolExecutionResult {

        val success =
            performGlobalAction(
                GLOBAL_ACTION_HOME
            )

        return ToolExecutionResult(
            success,
            if (success)
                "Navigated home, sir."
            else
                "Failed to navigate home."
        )
    }

    fun performGoBack():
            ToolExecutionResult {

        val success =
            performGlobalAction(
                GLOBAL_ACTION_BACK
            )

        return ToolExecutionResult(
            success,
            if (success)
                "Went back, sir."
            else
                "Failed to go back."
        )
    }

    fun performOpenRecents():
            ToolExecutionResult {

        val success =
            performGlobalAction(
                GLOBAL_ACTION_RECENTS
            )

        return ToolExecutionResult(
            success,
            if (success)
                "Opened recent apps, sir."
            else
                "Failed to open recent apps."
        )
    }

    fun performTakeScreenshot():
            ToolExecutionResult {

        val success =
            performGlobalAction(
                GLOBAL_ACTION_TAKE_SCREENSHOT
            )

        return ToolExecutionResult(
            success,
            if (success)
                "Screenshot captured, sir."
            else
                "Failed to capture screenshot."
        )
    }

    // ------------------------------------------------------------
    // GESTURES
    // ------------------------------------------------------------

    fun performScrollDown():
            ToolExecutionResult {

        val metrics =
            resources.displayMetrics

        val width =
            metrics.widthPixels.toFloat()

        val height =
            metrics.heightPixels.toFloat()

        val path =
            Path().apply {

                moveTo(
                    width / 2f,
                    height * 0.75f
                )

                lineTo(
                    width / 2f,
                    height * 0.25f
                )
            }

        val gesture =
            GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        400
                    )
                )
                .build()

        val success =
            dispatchGesture(
                gesture,
                null,
                null
            )

        return ToolExecutionResult(
            success,
            if (success)
                "Scrolled down, sir."
            else
                "Scroll failed."
        )
    }

    fun performScrollUp():
            ToolExecutionResult {

        val metrics =
            resources.displayMetrics

        val width =
            metrics.widthPixels.toFloat()

        val height =
            metrics.heightPixels.toFloat()

        val path =
            Path().apply {

                moveTo(
                    width / 2f,
                    height * 0.25f
                )

                lineTo(
                    width / 2f,
                    height * 0.75f
                )
            }

        val gesture =
            GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        400
                    )
                )
                .build()

        val success =
            dispatchGesture(
                gesture,
                null,
                null
            )

        return ToolExecutionResult(
            success,
            if (success)
                "Scrolled up, sir."
            else
                "Scroll failed."
        )
    }

    // ------------------------------------------------------------
    // CLICK
    // ------------------------------------------------------------

    fun findAndClickElement(
        query: String
    ): ToolExecutionResult {

        val root =
            rootInActiveWindow
                ?: return ToolExecutionResult(
                    false,
                    "Screen content unavailable."
                )

        val node =
            findBestNode(
                root,
                query
            )

        if (node == null) {

            return ToolExecutionResult(
                false,
                "Could not find '$query'."
            )
        }

        val success =
            clickNode(node)

        return ToolExecutionResult(
            success,
            if (success)
                "Clicked '$query'."
            else
                "Found '$query' but could not click it."
        )
    }

    fun findAndLongClickElement(
        query: String
    ): ToolExecutionResult {

        val root =
            rootInActiveWindow
                ?: return ToolExecutionResult(
                    false,
                    "Screen unavailable."
                )

        val node =
            findBestNode(
                root,
                query
            )
                ?: return ToolExecutionResult(
                    false,
                    "Could not find '$query'."
                )

        var target =
            node

        while (
            target != null &&
            !target.isLongClickable
        ) {
            target =
                target.parent
        }

        if (target == null) {

            return ToolExecutionResult(
                false,
                "Element is not long-clickable."
            )
        }

        val success =
            target.performAction(
                AccessibilityNodeInfo.ACTION_LONG_CLICK
            )

        return ToolExecutionResult(
            success,
            if (success)
                "Long clicked '$query'."
            else
                "Long click failed."
        )
    }

    // ------------------------------------------------------------
    // TEXT INPUT
    // ------------------------------------------------------------

    fun typeTextIntoField(
        query: String,
        text: String
    ): ToolExecutionResult {

        val root =
            rootInActiveWindow
                ?: return ToolExecutionResult(
                    false,
                    "Screen unavailable."
                )

        val node =
            if (query.isBlank()) {
                findFocusedInput(root)
            } else {
                findBestNode(root, query)
                    ?: findFocusedInput(root)
            }

        if (node == null) {

            return ToolExecutionResult(
                false,
                "No editable text field found."
            )
        }

        val arguments =
            Bundle().apply {

                putCharSequence(
                    AccessibilityNodeInfo
                        .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }

        var success =
            node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                arguments
            )

        if (!success) {

            success =
                node.performAction(
                    AccessibilityNodeInfo.ACTION_FOCUS
                )

            if (success) {

                success =
                    node.performAction(
                        AccessibilityNodeInfo.ACTION_SET_TEXT,
                        arguments
                    )
            }
        }

        return ToolExecutionResult(
            success,
            if (success)
                "Text entered successfully."
            else
                "Could not enter text."
        )
    }

    // ------------------------------------------------------------
    // SCREEN READER
    // ------------------------------------------------------------

    fun readVisibleScreenText():
            ToolExecutionResult {

        val root =
            rootInActiveWindow
                ?: return ToolExecutionResult(
                    false,
                    "Screen content unavailable."
                )

        val values =
            mutableListOf<String>()

        collectText(
            root,
            values
        )

        val text =
            values
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(40)
                .joinToString(" | ")

        return ToolExecutionResult(
            true,
            if (text.isBlank())
                "No readable text found."
            else
                "Screen content: $text",
            mapOf(
                "text" to text
            )
        )
    }

    // ------------------------------------------------------------
    // WHATSAPP HELPERS
    // ------------------------------------------------------------

    fun findWhatsAppSearchButton():
            Boolean {

        val root =
            rootInActiveWindow
                ?: return false

        val candidates =
            listOf(
                "Search",
                "search",
                "Search…",
                "Search...",
                "Search chats"
            )

        for (candidate in candidates) {

            val node =
                findBestNode(
                    root,
                    candidate
                )

            if (
                node != null &&
                clickNode(node)
            ) {
                return true
            }
        }

        // Fallback: content descriptions
        val searchNode =
            findNodeByContentDescription(
                root,
                "search"
            )

        return searchNode != null &&
                clickNode(searchNode)
    }

    fun findWhatsAppChat(
        contactName: String
    ): Boolean {

        val root =
            rootInActiveWindow
                ?: return false

        val node =
            findBestNode(
                root,
                contactName
            )
                ?: return false

        return clickNode(node)
    }

    fun findWhatsAppMessageInput():
            AccessibilityNodeInfo? {

        val root =
            rootInActiveWindow
                ?: return null

        val inputs =
            mutableListOf<AccessibilityNodeInfo>()

        collectEditableNodes(
            root,
            inputs
        )

        if (inputs.isNotEmpty()) {

            // WhatsApp message box is usually the
            // lowest visible editable field.
            return inputs.last()
        }

        val candidates =
            listOf(
                "Type a message",
                "Message",
                "Type a message..."
            )

        for (candidate in candidates) {

            val node =
                findBestNode(
                    root,
                    candidate
                )

            if (node != null) {
                return node
            }
        }

        return null
    }

    fun setTextOnNode(
        node: AccessibilityNodeInfo,
        text: String
    ): Boolean {

        val args =
            Bundle().apply {

                putCharSequence(
                    AccessibilityNodeInfo
                        .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }

        return node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            args
        )
    }

    fun clickWhatsAppSend():
            Boolean {

        val root =
            rootInActiveWindow
                ?: return false

        val sendNames =
            listOf(
                "Send",
                "send",
                "Send message"
            )

        for (name in sendNames) {

            val node =
                findBestNode(
                    root,
                    name
                )

            if (
                node != null &&
                clickNode(node)
            ) {
                return true
            }
        }

        val descriptions =
            listOf(
                "Send",
                "send message",
                "Send message"
            )

        for (description in descriptions) {

            val node =
                findNodeByContentDescription(
                    root,
                    description
                )

            if (
                node != null &&
                clickNode(node)
            ) {
                return true
            }
        }

        return false
    }

    // ------------------------------------------------------------
    // NODE SEARCH
    // ------------------------------------------------------------

    private fun findBestNode(
        root: AccessibilityNodeInfo,
        query: String
    ): AccessibilityNodeInfo? {

        val normalized =
            query
                .trim()
                .lowercase(Locale.getDefault())

        if (normalized.isBlank()) {
            return null
        }

        val allNodes =
            mutableListOf<AccessibilityNodeInfo>()

        collectNodes(
            root,
            allNodes
        )

        // Exact text first
        allNodes.forEach { node ->

            val text =
                node.text
                    ?.toString()
                    ?.trim()
                    ?.lowercase(
                        Locale.getDefault()
                    )

            if (text == normalized) {
                return node
            }
        }

        // Content description exact
        allNodes.forEach { node ->

            val description =
                node.contentDescription
                    ?.toString()
                    ?.trim()
                    ?.lowercase(
                        Locale.getDefault()
                    )

            if (description == normalized) {
                return node
            }
        }

        // Contains
        allNodes.forEach { node ->

            val text =
                node.text
                    ?.toString()
                    ?.trim()
                    ?.lowercase(
                        Locale.getDefault()
                    )

            val description =
                node.contentDescription
                    ?.toString()
                    ?.trim()
                    ?.lowercase(
                        Locale.getDefault()
                    )

            if (
                text?.contains(normalized) == true ||
                description?.contains(normalized) == true
            ) {
                return node
            }
        }

        return null
    }

    private fun findNodeByContentDescription(
        root: AccessibilityNodeInfo,
        query: String
    ): AccessibilityNodeInfo? {

        val normalized =
            query
                .trim()
                .lowercase(
                    Locale.getDefault()
                )

        val nodes =
            mutableListOf<AccessibilityNodeInfo>()

        collectNodes(
            root,
            nodes
        )

        return nodes.firstOrNull {

            it.contentDescription
                ?.toString()
                ?.lowercase(
                    Locale.getDefault()
                )
                ?.contains(normalized) == true
        }
    }

    private fun findFocusedInput(
        root: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        val focused =
            root.findFocus(
                AccessibilityNodeInfo.FOCUS_INPUT
            )

        if (
            focused != null &&
            focused.isEditable
        ) {
            return focused
        }

        val inputs =
            mutableListOf<AccessibilityNodeInfo>()

        collectEditableNodes(
            root,
            inputs
        )

        return inputs.lastOrNull()
    }

    private fun collectEditableNodes(
        node: AccessibilityNodeInfo?,
        result: MutableList<AccessibilityNodeInfo>
    ) {

        if (node == null) return

        if (node.isEditable) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {

            collectEditableNodes(
                node.getChild(i),
                result
            )
        }
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo?,
        result: MutableList<AccessibilityNodeInfo>
    ) {

        if (node == null) return

        result.add(node)

        for (i in 0 until node.childCount) {

            collectNodes(
                node.getChild(i),
                result
            )
        }
    }

    private fun clickNode(
        node: AccessibilityNodeInfo
    ): Boolean {

        var target:
                AccessibilityNodeInfo? = node

        while (
            target != null &&
            !target.isClickable
        ) {
            target = target.parent
        }

        return target?.performAction(
            AccessibilityNodeInfo.ACTION_CLICK
        ) == true
    }

    private fun collectText(
        node: AccessibilityNodeInfo?,
        result: MutableList<String>
    ) {

        if (node == null) return

        node.text
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?.let {
                result.add(it)
            }

        node.contentDescription
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?.let {
                result.add(it)
            }

        for (i in 0 until node.childCount) {

            collectText(
                node.getChild(i),
                result
            )
        }
    }

    companion object {

        @Volatile
        var instance:
                JarvisAccessibilityService? = null
            private set

        fun isServiceAvailable():
                Boolean =
            instance != null
    }
}
