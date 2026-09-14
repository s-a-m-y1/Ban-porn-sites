package com.contentfilter.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * First-launch welcome: the app introduces itself, asks for a name (optional),
 * and marks the start of the user's journey. Runs once, right after the splash.
 */
class WelcomeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // page rhythm: every element rises one beat after the last
        findViewById<LinearLayout>(R.id.welcomeContainer)?.let { container ->
            val children = (0 until container.childCount).map { container.getChildAt(it) }
            UiAnim.staggeredEntrance(*children.toTypedArray())
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val name = findViewById<EditText>(R.id.nameInput).text.toString().trim()
            prefs.edit()
                .putString("user_name", name.ifEmpty { null })
                .putString("first_open_day", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
                .putBoolean("onboarding_done", true)
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(R.anim.f7_activity_enter, R.anim.f7_activity_exit)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // no way back into the splash loop; treat back as "start without a name"
        findViewById<Button>(R.id.startButton).performClick()
    }
}
