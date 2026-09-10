package com.contentfilter.app

import android.content.Context

object StatsTracker {
    private var blockedCount = 0L

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
     * Called from the VPN service's IO scope. Reporting is best-effort:
     * failures (offline, backend down) are silently ignored.
     */
    suspend fun recordBlocked(context: Context, domain: String) {
        blockedCount++
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

    fun totalBlocked(): Long = blockedCount
}

data class ReportBlockedDto(val deviceId: String, val domain: String)
