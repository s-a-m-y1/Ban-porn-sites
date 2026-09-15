package com.contentfilter.app

import android.content.Context

/**
 * P1-1: Behavior-adaptive tracker — how many times the user hit a blocked
 * domain recently. Stored in SharedPreferences, per-day bucket + per-category.
 * Used to evolve the overlay: gentle → different → reflection → challenge.
 */
object BlockAttemptTracker {

    private const val PREFS = "hisn_attempts"
    private const val KEY_TOTAL_TODAY = "total_today"
    private const val KEY_DATE = "date_yyyyMMdd"

    fun incrementAndGet(context: Context, category: String): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = todayKey()
        val lastDate = prefs.getString(KEY_DATE, null)
        val editor = prefs.edit()
        if (lastDate != today) {
            editor.putString(KEY_DATE, today)
            editor.putInt(KEY_TOTAL_TODAY, 0)
            // reset per-category counters for new day
            for (cat in listOf("porn", "gambling", "adult-content")) {
                editor.remove("cat_$cat")
            }
        }
        val total = prefs.getInt(KEY_TOTAL_TODAY, 0) + 1
        val catKey = "cat_${category.lowercase()}"
        val catCount = prefs.getInt(catKey, 0) + 1
        editor.putInt(KEY_TOTAL_TODAY, total)
        editor.putInt(catKey, catCount)
        editor.apply()
        return catCount
    }

    fun totalToday(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_TOTAL_TODAY, 0)

    private fun todayKey(): String {
        val c = java.util.Calendar.getInstance()
        return "%04d%02d%02d".format(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
    }
}
