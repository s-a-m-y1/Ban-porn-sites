package com.contentfilter.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * P1-2: Replaces AppLockService 700ms polling with event-driven <100ms overlay.
 * Listens for TYPE_WINDOW_STATE_CHANGED — the moment a blocked app comes to
 * foreground, we launch AppLockActivity before its content is meaningfully visible.
 * Falls back gracefully if accessibility service is disabled — AppLockService polling remains.
 */
class HisnAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return // ignore self
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val blocked = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
        if (pkg in blocked) {
            val intent = Intent(this, AppLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("blocked_app", pkg)
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onInterrupt() {}
}
