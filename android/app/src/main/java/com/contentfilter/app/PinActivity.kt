package com.contentfilter.app

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView

/**
 * PIN gate: shown before stopping protection or changing security settings.
 * Default PIN is 1234 until the user changes it.
 */
class PinActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REASON = "reason"
        fun intent(context: Context, reason: String) =
            Intent(context, PinActivity::class.java).apply {
                putExtra(EXTRA_REASON, reason)
                // result handled via prefs flag
            }
    }

    private var attempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val reason = intent.getStringExtra(EXTRA_REASON) ?: ""
        findViewById<TextView>(R.id.pinReason).text = reason

        val input = findViewById<EditText>(R.id.pinInput)
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) verify()
            false
        }
        findViewById<View>(R.id.pinConfirm).setOnClickListener { verify() }
    }

    private fun verify() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val pin = prefs.getString("pin", "1234")!!
        val entered = findViewById<EditText>(R.id.pinInput).text.toString()
        if (entered == pin) {
            prefs.edit().putBoolean("pin_verified", true).apply()
            setResult(RESULT_OK)
            finish()
        } else {
            attempts++
            findViewById<TextView>(R.id.pinError).visibility = View.VISIBLE
            findViewById<EditText>(R.id.pinInput).setText("")
            if (attempts >= 5) {
                // too many wrong attempts: kick to home
                val home = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(home)
                finish()
            }
        }
    }
}
