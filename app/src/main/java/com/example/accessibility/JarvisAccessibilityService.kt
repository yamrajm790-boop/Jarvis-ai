package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.tools.ToolExecutionResult

class JarvisAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Safe operation: No passive user tracking or recording of events
    }

    override fun onInterrupt() {
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun performGoHome(): ToolExecutionResult {
        val success = performGlobalAction(GLOBAL_ACTION_HOME)
        return ToolExecutionResult(success, if (success) "Navigated home, sir." else "Failed to navigate home.")
    }

    fun performGoBack(): ToolExecutionResult {
        val success = performGlobalAction(GLOBAL_ACTION_BACK)
        return ToolExecutionResult(success, if (success) "Executed back gesture, sir." else "Failed to perform back gesture.")
    }

    fun performOpenRecents(): ToolExecutionResult {
        val success = performGlobalAction(GLOBAL_ACTION_RECENTS)
        return ToolExecutionResult(success, if (success) "Opened recent apps, sir." else "Failed to open recent apps.")
    }

    fun performTakeScreenshot(): ToolExecutionResult {
        val success = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        return ToolExecutionResult(success, if (success) "Screenshot captured, sir." else "Failed to trigger system screenshot.")
    }

    fun performScrollDown(): ToolExecutionResult {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val path = Path().apply {
            moveTo(width / 2f, height * 0.7f)
            lineTo(width / 2f, height * 0.3f)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        val dispatched = dispatchGesture(gesture, null, null)
        return ToolExecutionResult(dispatched, if (dispatched) "Scrolled down, sir." else "Scroll gesture unavailable.")
    }

    fun performScrollUp(): ToolExecutionResult {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val path = Path().apply {
            moveTo(width / 2f, height * 0.3f)
            lineTo(width / 2f, height * 0.7f)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        val dispatched = dispatchGesture(gesture, null, null)
        return ToolExecutionResult(dispatched, if (dispatched) "Scrolled up, sir." else "Scroll gesture unavailable.")
    }

    fun findAndClickElement(textOrId: String): ToolExecutionResult {
        val root = rootInActiveWindow ?: return ToolExecutionResult(false, "Screen content unavailable.")
        val node = findNodeByTextOrId(root, textOrId)
        if (node != null) {
            val clicked = performClickAction(node)
            return ToolExecutionResult(clicked, if (clicked) "Clicked '$textOrId', sir." else "Element found but click failed.")
        }
        return ToolExecutionResult(false, "Could not locate element '$textOrId' on screen.")
    }

    fun findAndLongClickElement(textOrId: String): ToolExecutionResult {
        val root = rootInActiveWindow ?: return ToolExecutionResult(false, "Screen content unavailable.")
        val node = findNodeByTextOrId(root, textOrId)
        if (node != null) {
            var target: AccessibilityNodeInfo? = node
            while (target != null && !target.isLongClickable) {
                target = target.parent
            }
            if (target != null && target.isLongClickable) {
                val success = target.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                return ToolExecutionResult(success, if (success) "Long clicked '$textOrId', sir." else "Long click failed.")
            }
        }
        return ToolExecutionResult(false, "Could not long click element '$textOrId'.")
    }

    fun typeTextIntoField(textOrId: String, textToType: String): ToolExecutionResult {
        val root = rootInActiveWindow ?: return ToolExecutionResult(false, "Screen content unavailable.")
        val node = findNodeByTextOrId(root, textOrId) ?: root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (node != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            return ToolExecutionResult(success, if (success) "Typed into '$textOrId', sir." else "Failed to set text.")
        }
        return ToolExecutionResult(false, "Could not find text input field.")
    }

    fun readVisibleScreenText(): ToolExecutionResult {
        val root = rootInActiveWindow ?: return ToolExecutionResult(false, "Screen content unavailable, sir.")
        val textList = mutableListOf<String>()
        collectTextFromNode(root, textList)
        val fullText = textList.distinct().filter { it.isNotBlank() }.take(15).joinToString(" | ")
        if (fullText.isBlank()) {
            return ToolExecutionResult(true, "Screen is open, but no clear text elements were detected, sir.")
        }
        return ToolExecutionResult(true, "Screen content summary: $fullText", mapOf("text" to fullText))
    }

    private fun findNodeByTextOrId(root: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val byText = root.findAccessibilityNodeInfosByText(query)
        if (!byText.isNullOrEmpty()) {
            return byText.first()
        }
        val byViewId = root.findAccessibilityNodeInfosByViewId(query)
        if (!byViewId.isNullOrEmpty()) {
            return byViewId.first()
        }
        return null
    }

    private fun performClickAction(node: AccessibilityNodeInfo): Boolean {
        var target: AccessibilityNodeInfo? = node
        while (target != null && !target.isClickable) {
            target = target.parent
        }
        return target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    private fun collectTextFromNode(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (!text.isNullOrBlank()) {
            list.add(text.trim())
        }
        for (i in 0 until node.childCount) {
            collectTextFromNode(node.getChild(i), list)
        }
    }

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set

        fun isServiceAvailable(): Boolean = instance != null
    }
}
