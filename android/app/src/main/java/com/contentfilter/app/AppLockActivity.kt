package com.contentfilter.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

/**
 * Full-screen lock shown over a blocked app. Finishes back to home.
 */
class AppLockActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_lock)

        val blockedSite = intent.getStringExtra("blocked_site")
        val blockedApp = intent.getStringExtra("blocked_app")
        if (blockedSite != null) {
            // triggered by the VPN when a blocked domain is queried
            findViewById<TextView>(R.id.lockedAppName).text = blockedSite
            findViewById<TextView>(R.id.blockedMessage).setText(R.string.blocked_site_message)
        } else {
            val appLabel = try {
                val pm = packageManager
                val info = pm.getApplicationInfo(blockedApp ?: "", 0)
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                getString(R.string.blocked_app_default)
            }
            findViewById<TextView>(R.id.lockedAppName).text = appLabel
        }

        val backHome = findViewById<Button>(R.id.backHomeButton)
        backHome.setOnClickListener {
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(home)
            finish()
        }

        // calm staged entrance: icon → reminder → rule → context → action
        UiAnim.staggeredEntrance(
            findViewById<View>(R.id.lockIcon),
            findViewById<View>(R.id.lockHero),
            findViewById<View>(R.id.lockRule),
            findViewById<View>(R.id.lockedAppName),
            findViewById<View>(R.id.blockedMessage),
            backHome,
        )
        UiAnim.pressable(backHome)
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
