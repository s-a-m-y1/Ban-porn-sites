package com.contentfilter.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Turns protection ON/OFF automatically at scheduled times.
 * Schedule format: startHour:startMin - endHour:endMin (24h).
 * If now is inside the window -> protection ON, else OFF.
 */
object ScheduleManager {

    const val ACTION_SYNC_SCHEDULE = "com.contentfilter.app.SYNC_SCHEDULE"

    fun scheduleEnabled(context: Context): Boolean =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("schedule_enabled", false)

    fun window(context: Context): Pair<Int, Int>? {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("schedule_enabled", false)) return null
        val startMin = prefs.getInt("schedule_start", -1)
        val endMin = prefs.getInt("schedule_end", -1)
        if (startMin < 0 || endMin < 0) return null
        return startMin to endMin
    }

    fun inWindowNow(startMin: Int, endMin: Int): Boolean {
        val cal = Calendar.getInstance()
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return if (startMin <= endMin) now in startMin..endMin
        else now >= startMin || now <= endMin // window crosses midnight
    }

    /** Apply current schedule state: start/stop the VPN accordingly. */
    fun apply(context: Context) {
        val win = window(context) ?: return
        val shouldRun = inWindowNow(win.first, win.second)
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("vpn_running", false)
        if (shouldRun && !isRunning) {
            FilterVpnService.start(context)
            prefs.edit().putBoolean("vpn_running", true).apply()
        } else if (!shouldRun && isRunning) {
            FilterVpnService.stop(context)
            prefs.edit().putBoolean("vpn_running", false).apply()
        }
    }

    fun setAlarms(context: Context) {
        val win = window(context) ?: return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startIntent = PendingIntent.getBroadcast(
            context, 1001,
            Intent(context, ScheduleReceiver::class.java).apply {
                action = ScheduleReceiver.ACTION_START
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getBroadcast(
            context, 1002,
            Intent(context, ScheduleReceiver::class.java).apply {
                action = ScheduleReceiver.ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val (startMin, endMin) = win
        val calStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startMin / 60)
            set(Calendar.MINUTE, startMin % 60)
            set(Calendar.SECOND, 0)
        }
        val calStop = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endMin / 60)
            set(Calendar.MINUTE, endMin % 60)
            set(Calendar.SECOND, 0)
        }
        if (calStart.timeInMillis <= System.currentTimeMillis()) {
            calStart.add(Calendar.DAY_OF_YEAR, 1)
        }
        if (calStop.timeInMillis <= System.currentTimeMillis()) {
            calStop.add(Calendar.DAY_OF_YEAR, 1)
        }

        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, calStart.timeInMillis,
            AlarmManager.INTERVAL_DAY, startIntent,
        )
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, calStop.timeInMillis,
            AlarmManager.INTERVAL_DAY, stopIntent,
        )
    }
}

class ScheduleReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_START = "com.contentfilter.app.SCHEDULE_START"
        const val ACTION_STOP = "com.contentfilter.app.SCHEDULE_STOP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_START -> {
                FilterVpnService.start(context)
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("vpn_running", true).apply()
            }
            ACTION_STOP -> {
                FilterVpnService.stop(context)
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("vpn_running", false).apply()
            }
        }
        // keep app lock in sync with protection state
        if (AppPrefs.appLockEnabled(context)) {
            AppLockService.start(context)
        }
    }
}
