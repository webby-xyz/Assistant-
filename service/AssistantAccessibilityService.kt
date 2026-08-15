package com.yourapp.assistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AssistantAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: AssistantAccessibilityService? = null
    }

    override fun onServiceConnected() { instance = this; super.onServiceConnected() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { instance = null; super.onDestroy() }

    // ---------- Gestures ----------
    private fun gesture(path: Path, duration: Long) {
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        dispatchGesture(g, null, null)
    }

    fun tap(x: Int, y: Int) {
        val p = Path().apply { moveTo(x.toFloat(), y.toFloat()); lineTo(x.toFloat(), y.toFloat()) }
        gesture(p, 80)
    }

    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = 400) {
        val p = Path().apply { moveTo(x1.toFloat(), y1.toFloat()); lineTo(x2.toFloat(), y2.toFloat()) }
        gesture(p, durationMs)
    }

    // ---------- Screen padhna (LLM ko dikhata hai) ----------
    fun getScreenText(): String {
        val root = rootInActiveWindow ?: return "(screen khali hai)"
        val sb = StringBuilder()
        val seen = HashSet<String>()
        try {
            fun walk(n: AccessibilityNodeInfo, depth: Int) {
                if (sb.length > 8000) return
                val text = n.text?.toString() ?: ""
                val desc = n.contentDescription?.toString() ?: ""
                val clickable = n.isClickable
                val r = Rect(); n.getBoundsInScreen(r)
                val label = (if (text.isNotEmpty()) "T:$text " else "") +
                            (if (desc.isNotEmpty()) "D:$desc " else "") +
                            (if (clickable) "[tap@${r.centerX()},${r.centerY()}]" else "")
                if (label.trim().isNotEmpty() && seen.add(label)) {
                    sb.append("  ".repeat(depth.coerceAtMost(4))).append(label).append('\n')
                }
                for (i in 0 until n.childCount) n.getChild(i)?.let { walk(it, depth + 1) }
            }
            walk(root, 0)
        } catch (_: Exception) {}
        return sb.toString().ifEmpty { "(screen par text nahi mila)" }
    }

    // ---------- Text par click ----------
    fun clickText(text: String): String {
        val root = rootInActiveWindow ?: return "ERROR: Screen nahi mili"
        val target = findNode(root) {
            it.text?.toString()?.contains(text, true) == true ||
            it.contentDescription?.toString()?.contains(text, true) == true
        } ?: return "ERROR: '$text' screen par nahi mila"
        val r = Rect(); target.getBoundsInScreen(r)
        tap(r.centerX(), r.centerY())
        return "'$text' par click kar diya"
    }

    // ---------- Type karna ----------
    fun typeText(text: String): String {
        val root = rootInActiveWindow ?: return "ERROR: Screen nahi mili"
        val field = findNode(root) { it.className?.toString()?.contains("EditText", true) == true }
            ?: return "ERROR: Text field nahi mila"
        val bundle = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        return "Type kar diya: $text"
    }

    // ---------- Global buttons ----------
    fun pressGlobal(action: Int): String {
        performGlobalAction(action)
        return when (action) {
            GLOBAL_ACTION_BACK -> "Back dabaya"
            GLOBAL_ACTION_HOME -> "Home chala gaya"
            GLOBAL_ACTION_RECENTS -> "Recents khol diye"
            else -> "Action ho gaya"
        }
    }

    // ---------- Scroll ----------
    fun scroll(direction: String): String {
        val root = rootInActiveWindow ?: return "ERROR: Screen nahi mili"
        val area = findNode(root) { it.isScrollable } ?: return "ERROR: Scrollable area nahi mila"
        val r = Rect(); area.getBoundsInScreen(r)
        val cx = r.centerX(); val cy = r.centerY()
        val dy = (r.height() * 0.7).toInt().coerceAtLeast(200)
        when (direction.lowercase()) {
            "up", "top", "upper" -> swipe(cx, cy + dy, cx, cy - dy)
            "down", "bottom", "lower" -> swipe(cx, cy - dy, cx, cy + dy)
            else -> return "ERROR: direction 'up' ya 'down' do"
        }
        return "Scroll kar diya ($direction)"
    }

    // ---------- Helper ----------
    private fun findNode(node: AccessibilityNodeInfo, pred: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        try {
            if (pred(node)) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    findNode(child, pred)?.let { return it }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
