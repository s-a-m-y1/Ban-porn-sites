package com.contentfilter.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings

/**
 * Monitors the foreground app; if it's in the blocked-apps list,
 * launches a lock overlay activity on top of it.
 */
class AppLockService : Service() {

    companion object {
        const val CHANNEL_ID = "app_lock_channel"
        private const val CHECK_INTERVAL = 700L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, AppLockService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppLockService::class.java))
        }

        fun blockedApps(context: Context): Set<String> {
            return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getStringSet("blocked_apps", emptySet()) ?: emptySet()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStats: UsageStatsManager

    private val checker = object : Runnable {
        override fun run() {
            if (hasUsageAccess()) {
                val fg = foregroundApp()
                if (fg != null && fg in blockedApps(applicationContext) && fg != packageName) {
                    val intent = Intent(applicationContext, AppLockActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        putExtra("blocked_app", fg)
                    }
                    startActivity(intent)
                }
            }
            handler.postDelayed(this, CHECK_INTERVAL)
        }
    }

    override fun onCreate() {
        super.onCreate()
        usageStats = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        startForeground(2, buildNotification())
        handler.post(checker)
    }

    override fun onDestroy() {
        handler.removeCallbacks(checker)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasUsageAccess(): Boolean {
        val granted = packageManager.queryIntentActivities(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), 0,
        ).isNotEmpty()
        if (!granted) return true // feature not supported on device: skip check silently
        try {
            val now = System.currentTimeMillis()
            usageStats.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 1000, now)
            val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(), packageName,
            )
            return mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            return false
        }
    }

    private fun foregroundApp(): String? {
        val now = System.currentTimeMillis()
        val stats = usageStats.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, now - 5000, now,
        )
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID, "App Lock", NotificationManager.IMPORTANCE_MIN,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Content Filter")
            .setContentText("App protection active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .build()
    }
}
