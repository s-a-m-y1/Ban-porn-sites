package com.contentfilter.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PIN gate: shown before stopping protection or changing security settings.
 * There is no default PIN — nothing verifies until a PIN is set in Settings.
 *
 * UX: a hidden field captures keystrokes, animated dots render them, the dots
 * shake on a wrong PIN, glow teal on success, and auto-submit at 6 digits.
 */
class PinActivity : BaseActivity() {

    companion object {
        const val EXTRA_REASON = "reason"
        fun intent(context: Context, reason: String) =
            Intent(context, PinActivity::class.java).apply {
                putExtra(EXTRA_REASON, reason)
                // result handled via prefs flag
            }

        private const val MIN_PIN_LENGTH = 4
        private const val MAX_PIN_LENGTH = 6
    }

    private var attempts = 0
    private lateinit var input: EditText
    private lateinit var dots: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val reason = intent.getStringExtra(EXTRA_REASON) ?: ""
        findViewById<TextView>(R.id.pinReason).text = reason

        dots = listOf(
            findViewById(R.id.dot1), findViewById(R.id.dot2),
            findViewById(R.id.dot3), findViewById(R.id.dot4),
            findViewById(R.id.dot5), findViewById(R.id.dot6),
        )

        // capture field sits transparently over the dots — tapping anywhere
        // in that area focuses it; the keypad follows the focus
        input = findViewById(R.id.pinInput)
        input.setOnClickListener { showKeypad() }
        input.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showKeypad() }

        // raise the keypad right away so the user can start typing immediately.
        // Some IMEs ignore a request made before the window is fully shown,
        // so retry on window focus and once more shortly after.
        input.post { showKeypad() }
        input.postDelayed({ if (!isFinishing) showKeypad() }, 400)
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE,
        )

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                renderDots(s?.length ?: 0)
                // 6 digits is always a complete PIN — submit without a tap
                if ((s?.length ?: 0) == MAX_PIN_LENGTH) verify()
            }
        })
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) verify()
            false
        }
        findViewById<View>(R.id.pinConfirm).setOnClickListener { verify() }
    }

    private fun showKeypad() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // IMEs reliably honor the request once we truly have window focus
        if (hasFocus) input.post { showKeypad() }
    }

    private fun renderDots(length: Int) {
        dots.forEachIndexed { i, dot ->
            // dots 5/6 only appear once the first four are filled — the row
            // breathes with the PIN's real length (PINs are 4–6 digits)
            dot.visibility = if (i < MAX_PIN_LENGTH && (i < MIN_PIN_LENGTH || length > MIN_PIN_LENGTH)) {
                View.VISIBLE
            } else {
                View.GONE
            }
            dot.setBackgroundResource(
                when {
                    i < length -> R.drawable.pin_dot_filled
                    else -> R.drawable.pin_dot_empty
                },
            )
            if (i == length) {
                dot.animate().scaleX(1.25f).scaleY(1.25f).setDuration(90)
                    .withEndAction { dot.animate().scaleX(1f).scaleY(1f).setDuration(90).start() }
                    .start()
            }
        }
    }

    private fun verify() {
        val entered = input.text.toString()
        if (entered.length < MIN_PIN_LENGTH) return
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val ok = PinManager.isPinSet(this) && PinManager.verify(this, entered)
        if (ok) {
            prefs.edit().putBoolean("pin_verified", true).apply()
            // teal flash, then hand the result back
            dots.forEach { it.setBackgroundResource(R.drawable.pin_dot_success) }
            haptic()
            CoroutineScope(Dispatchers.Main).launch {
                delay(180)
                setResult(RESULT_OK)
                finish()
            }
        } else {
            attempts++
            findViewById<TextView>(R.id.pinError).visibility = View.VISIBLE
            dots.forEach { it.setBackgroundResource(R.drawable.pin_dot_error) }
            shakeDots()
            haptic()
            input.setText("")
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

    private fun shakeDots() {
        val row = findViewById<LinearLayout>(R.id.dotsRow)
        android.animation.ObjectAnimator.ofFloat(
            row, "translationX", 0f, -26f, 26f, -18f, 18f, -8f, 8f, 0f,
        ).setDuration(480).start()
    }

    private fun haptic() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        } catch (_: Exception) {
            // haptics are garnish — never let them break the flow
        }
    }
}
