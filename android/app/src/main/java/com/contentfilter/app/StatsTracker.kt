package com.contentfilter.app

import android.content.Context

object StatsTracker {
    fun deviceId(context: Context): String {
        val prefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    /**
     * Persist total blocked count locally and report to backend best-effort.
     * Called from the VPN service's IO coroutine.
     */
    suspend fun recordBlocked(context: Context, domain: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val total = prefs.getInt("blocked_total", 0) + 1
        prefs.edit().putInt("blocked_total", total).apply()

        val api = ApiService.create(
            context.getSharedPreferences("blocklist_prefs", Context.MODE_PRIVATE)
                .getString("backend_url", "http://10.0.2.2:3000")!!,
        )
        try {
            api.reportBlocked(ReportBlockedDto(deviceId(context), domain))
        } catch (_: Exception) {
            // offline is fine - stats are best-effort
        }
    }
}

data class ReportBlockedDto(val deviceId: String, val domain: String)
