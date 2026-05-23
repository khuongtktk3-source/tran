package com.example.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * MacroAccessibilityService: Active Android AccessibilityService used to interact with
 * real physical device screens to perform clicks, swipes and back/home gestures automatically.
 */
class MacroAccessibilityService : AccessibilityService() {

    companion object {
        private var instanceRef = WeakReference<MacroAccessibilityService>(null)

        val instance: MacroAccessibilityService?
            get() = instanceRef.get()

        val isServiceActive: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        Log.d("MacroService", "Accessibility Service successfully linked dynamically to application system.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-operation needed as we focus on active gesture injections
    }

    override fun onInterrupt() {
        Log.e("MacroService", "Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instanceRef.clear()
        Log.d("MacroService", "Accessibility Service destroyed.")
    }

    /**
     * Performs an active, hardware-level simulated click on coordinates (x, y) on the real screen.
     */
    fun performClick(x: Float, y: Float, callback: ((Boolean) -> Unit)? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            callback?.invoke(false)
            return false
        }

        val path = Path().apply {
            moveTo(x, y)
        }

        try {
            val stroke = GestureDescription.StrokeDescription(path, 0, 80)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            return dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    callback?.invoke(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    callback?.invoke(false)
                }
            }, null)
        } catch (e: Exception) {
            Log.e("MacroService", "Failed to dispatch click gesture: ${e.localizedMessage}")
            callback?.invoke(false)
            return false
        }
    }

    /**
     * Performs a physical swipe gesture on the device screen.
     */
    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long, callback: ((Boolean) -> Unit)? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            callback?.invoke(false)
            return false
        }

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        try {
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(100))
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            return dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    callback?.invoke(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    callback?.invoke(false)
                }
            }, null)
        } catch (e: Exception) {
            Log.e("MacroService", "Failed to dispatch swipe gesture: ${e.localizedMessage}")
            callback?.invoke(false)
            return false
        }
    }

    /**
     * Performs global back command
     */
    fun performBackAction(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Performs global home button action
     */
    fun performHomeAction(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
