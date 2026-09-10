package com.contentfilter.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Categories {
    const val PORN = "porn"
    const val GAMBLING = "gambling"
    const val FAKENEWS = "fakenews"
    const val MALWARE = "malware"

    val ALL = listOf(PORN, GAMBLING, FAKENEWS, MALWARE)

    fun enabled(context: Context): List<String> {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val default = prefs.getStringSet("enabled_cats", null)
            ?: setOf(PORN, MALWARE) // porn + malware on by default
        return ALL.filter { it in default }
    }
}

object StatsTracker {
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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
     * Record a blocked attempt: persistent total + daily stats with top domains.
     * Called from the VPN service's IO coroutine.
     */
    suspend fun recordBlocked(context: Context, domain: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("blocked_total", prefs.getInt("blocked_total", 0) + 1).apply()

        val dao = BlocklistDatabase.getInstance(context).blockedDomainDao()
        val day = dayFmt.format(Date())
        val existing = dao.getStat(day)
        if (existing == null) {
            dao.upsertStat(DailyStat(day, 1, "$domain:1"))
        } else {
            val top = existing.topDomains.split(",")
                .mapNotNull { s ->
                    val parts = s.split(":")
                    if (parts.size == 2 && parts[0].isNotEmpty())
                        parts[0] to (parts[1].toIntOrNull() ?: 0)
                    else null
                }.toMutableList()
            val idx = top.indexOfFirst { it.first == domain }
            if (idx >= 0) top[idx] = top[idx].let { it.first to it.second + 1 }
            else top.add(domain to 1)
            val topStr = top.sortedByDescending { it.second }.take(5)
                .joinToString(",") { "${it.first}:${it.second}" }
            dao.upsertStat(existing.copy(blockedCount = existing.blockedCount + 1, topDomains = topStr))
        }

        val api = ApiService.create(
            context.getSharedPreferences("blocklist_prefs", Context.MODE_PRIVATE)
                .getString("backend_url", "http://10.0.2.2:3000")!!,
        )
        try {
            api.reportBlocked(ReportBlockedDto(deviceId(context), domain))
        } catch (_: Exception) {
        }
    }
}

data class ReportBlockedDto(val deviceId: String, val domain: String)
