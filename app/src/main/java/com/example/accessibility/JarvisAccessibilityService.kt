package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.example.tools.ToolExecutionResult

class JarvisAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Safe operation: No tracking or recording of events
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
        return ToolExecutionResult(success, if (success) "Going home, sir." else "Failed to navigate home.")
    }

    fun performGoBack(): ToolExecutionResult {
        val success = performGlobalAction(GLOBAL_ACTION_BACK)
        return ToolExecutionResult(success, if (success) "Going back, sir." else "Failed to perform back action.")
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
        return ToolExecutionResult(dispatched, if (dispatched) "Scrolling down, sir." else "Scroll gesture unavailable.")
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
        return ToolExecutionResult(dispatched, if (dispatched) "Scrolling up, sir." else "Scroll gesture unavailable.")
    }

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set

        fun isServiceAvailable(): Boolean = instance != null
    }
}
