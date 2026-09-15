package com.contentfilter.app

import android.content.Context
import android.provider.Settings
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * P2-5/Anti-disable: 15m local tamper watcher — checks protection prerequisites
 * and posts a local notification if any is revoked. No server telemetry.
 */
class TamperWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val p = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val vpnRunning = p.getBoolean("vpn_running", false)
        val strict = p.getBoolean("strict_mode", false)
        val applock = p.getBoolean("applock_enabled", false)

        // Only watch when protection expected
        if (!vpnRunning && !strict) return Result.success()

        val needOverlay = vpnRunning && !Settings.canDrawOverlays(applicationContext)
        val needUsage = applock && !hasUsageAccess()
        val needVpn = vpnRunning && !FilterVpnService.isRunning
        val privateDns = Settings.Global.getString(applicationContext.contentResolver, "private_dns_mode")
        val isPrivateDnsStrict = privateDns == "hostname" || privateDns == "opportunistic"

        if (needVpn || needOverlay || needUsage || isPrivateDnsStrict) {
            // Post local notification via TamperNotifier (simple)
            TamperNotifier.notify(applicationContext, needVpn, needOverlay, needUsage, isPrivateDnsStrict)
        }
        return Result.success()
    }

    private fun hasUsageAccess(): Boolean = try {
        val appOps = applicationContext.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), applicationContext.packageName)
        mode == android.app.AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }

    companion object {
        private const val WORK_NAME = "hisn_tamper_watch"
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<TamperWorker>(15, TimeUnit.MINUTES)
                .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

object TamperNotifier {
    fun notify(ctx: Context, needVpn: Boolean, needOverlay: Boolean, needUsage: Boolean, needPrivateDns: Boolean) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "hisn_tamper"
        val channel = android.app.NotificationChannel(channelId, "Hisn Tamper", android.app.NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Protection tamper alerts"
        }
        nm.createNotificationChannel(channel)
        val text = buildString {
            if (needVpn) append(ctx.getString(R.string.tamper_vpn) + " ")
            if (needOverlay) append(ctx.getString(R.string.tamper_overlay) + " ")
            if (needUsage) append(ctx.getString(R.string.tamper_usage) + " ")
            if (needPrivateDns) append(ctx.getString(R.string.tamper_private_dns))
        }.trim().ifEmpty { ctx.getString(R.string.tamper_generic) }
        val intent = android.app.PendingIntent.getActivity(ctx, 0, android.content.Intent(ctx, MainActivity::class.java), android.app.PendingIntent.FLAG_IMMUTABLE)
        val n = android.app.Notification.Builder(ctx, channelId)
            .setContentTitle(ctx.getString(R.string.tamper_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notif_transparent)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        nm.notify(2001, n)
    }
}
