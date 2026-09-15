package com.contentfilter.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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
 * UX: an on-screen keypad writes into a hidden capture field, designed dots
 * render the states (empty stroke, filled accent, error clay, success teal),
 * the row shakes on a wrong PIN, and the PIN auto-submits at 6 digits.
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
    private lateinit var errorRow: LinearLayout

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
        errorRow = findViewById(R.id.pinErrorRow)

        // capture field: a 1dp sink the on-screen keypad writes into — the
        // TextWatcher renders the dots and auto-verifies at 6 digits
        input = findViewById(R.id.pinInput)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // retyping dismisses the stale wrong-PIN error (presentation
                // only — the failure branch re-shows it after this pass)
                errorRow.visibility = View.GONE
                renderDots(s?.length ?: 0)
                // 6 digits is always a complete PIN — submit without a tap
                if ((s?.length ?: 0) == MAX_PIN_LENGTH) verify()
            }
        })

        bindKeypad()
        findViewById<View>(R.id.pinConfirm).setOnClickListener { verify() }
    }

    /** On-screen keypad: digits append to the capture field, backspace removes. */
    private fun bindKeypad() {
        val digitKeys = mapOf(
            R.id.key1 to '1', R.id.key2 to '2', R.id.key3 to '3',
            R.id.key4 to '4', R.id.key5 to '5', R.id.key6 to '6',
            R.id.key7 to '7', R.id.key8 to '8', R.id.key9 to '9',
            R.id.key0 to '0',
        )
        val keyViews = mutableListOf<View>()
        digitKeys.forEach { (id, digit) ->
            val key = findViewById<View>(id)
            key.setOnClickListener {
                appendDigit(digit)
                haptic()
            }
            keyViews.add(key)
        }
        val backspace = findViewById<View>(R.id.keyBackspace)
        backspace.setOnClickListener {
            deleteDigit()
            haptic()
        }
        keyViews.add(backspace)
        keyViews.add(findViewById(R.id.pinConfirm))
        UiAnim.pressable(*keyViews.toTypedArray())
    }

    private fun appendDigit(digit: Char) {
        val current = input.text.toString()
        if (current.length >= MAX_PIN_LENGTH) return
        input.setText(current + digit)
    }

    private fun deleteDigit() {
        val current = input.text.toString()
        if (current.isEmpty()) return
        input.setText(current.dropLast(1))
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
                    i < length -> R.drawable.f6_pin_dot_filled
                    else -> R.drawable.f6_pin_dot_empty
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
        // P0-5: persistent lockout (survives rotation/recreate)
        if (PinManager.isLockedOut(this)) {
            val ms = PinManager.remainingLockoutMs(this)
            val secs = (ms / 1000).coerceAtLeast(1)
            findViewById<TextView>(R.id.pinError)?.let { it.text = getString(R.string.pin_err_locked, secs) }
            errorRow.visibility = View.VISIBLE
            dots.forEach { it.setBackgroundResource(R.drawable.f6_pin_dot_error) }
            shakeDots()
            return
        }
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val ok = PinManager.isPinSet(this) && PinManager.verify(this, entered)
        if (ok) {
            prefs.edit().putBoolean("pin_verified", true).apply()
            PinManager.clearFailedAttempts(this)
            // teal flash, then hand the result back
            dots.forEach { it.setBackgroundResource(R.drawable.f6_pin_dot_success) }
            haptic()
            CoroutineScope(Dispatchers.Main).launch {
                delay(180)
                setResult(RESULT_OK)
                finish()
            }
        } else {
            attempts++
            PinManager.recordFailedAttempt(this)
            // clear first so the watcher's re-render doesn't overwrite the
            // clay state — the row then shakes in error color (UI-only
            // reorder; attempts / kick-home logic unchanged)
            input.setText("")
            if (PinManager.isLockedOut(this)) {
                val ms = PinManager.remainingLockoutMs(this)
                val secs = (ms / 1000).coerceAtLeast(1)
                findViewById<TextView>(R.id.pinError)?.let { it.text = getString(R.string.pin_err_locked, secs) }
            } else {
                findViewById<TextView>(R.id.pinError)?.let { it.text = getString(R.string.pin_err_wrong) }
            }
            errorRow.visibility = View.VISIBLE
            dots.forEach { it.setBackgroundResource(R.drawable.f6_pin_dot_error) }
            shakeDots()
            haptic()
            if (attempts >= 5 || PinManager.isLockedOut(this)) {
                // too many wrong attempts: kick to home (but respect lockout timer)
                if (attempts >= 5) {
                    val home = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(home)
                }
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
