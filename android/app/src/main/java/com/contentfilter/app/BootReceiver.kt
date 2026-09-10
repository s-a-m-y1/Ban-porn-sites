package com.contentfilter.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

                // autostart protection if enabled (default true)
                if (prefs.getBoolean("autostart", true)) {
                    ScheduleManager.apply(context) // honors schedule window if set
                    val win = ScheduleManager.window(context)
                    if (win == null) {
                        // no schedule: just start
                        FilterVpnService.start(context)
                        prefs.edit().putBoolean("vpn_running", true).apply()
                    }
                }

                // re-apply schedule alarms
                if (prefs.getBoolean("schedule_enabled", false)) {
                    ScheduleManager.setAlarms(context)
                }

                // restart app lock if enabled
                if (prefs.getBoolean("applock_enabled", false)) {
                    AppLockService.start(context)
                }
            }
        }
    }
}
