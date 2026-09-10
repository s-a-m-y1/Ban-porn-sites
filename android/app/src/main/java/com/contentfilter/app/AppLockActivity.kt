package com.contentfilter.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

/**
 * Full-screen lock shown over a blocked app. Finishes back to home.
 */
class AppLockActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_lock)

        val appLabel = try {
            val pm = packageManager
            val info = pm.getApplicationInfo(intent.getStringExtra("blocked_app") ?: "", 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            getString(R.string.blocked_app_default)
        }
        findViewById<TextView>(R.id.lockedAppName).text = appLabel

        findViewById<Button>(R.id.backHomeButton).setOnClickListener {
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(home)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // block going back to the blocked app
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(home)
        finish()
    }
}
