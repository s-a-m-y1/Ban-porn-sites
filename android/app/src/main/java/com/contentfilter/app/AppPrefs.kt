package com.contentfilter.app

import android.content.Context

object AppPrefs {
    fun pinEnabled(context: Context): Boolean =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("pin_enabled", false)

    fun appLockEnabled(context: Context): Boolean =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("applock_enabled", false)

    fun adminEnabled(context: Context): Boolean =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("admin_enabled", false)
}
