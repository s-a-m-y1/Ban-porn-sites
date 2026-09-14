package com.contentfilter.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Create / change the app PIN: entry + confirmation with live validation.
 *
 * UX: the shared on-screen keypad writes into two hidden capture fields,
 * each with its own labeled dot row. Tapping a row routes keypad input to
 * it, and the entry row hands over to confirmation once full. Validation
 * errors mark the offending row in clay with a shake. Validation flow
 * (watcher, save, showErr) is unchanged.
 */
class SetPinActivity : BaseActivity() {

    companion object {
        const val EXTRA_CHANGE = "change"
        fun intent(context: Context, change: Boolean = false) =
            Intent(context, SetPinActivity::class.java).apply { putExtra(EXTRA_CHANGE, change) }

        private const val MIN_PIN_LENGTH = 4
        private const val MAX_PIN_LENGTH = 6
    }

    private lateinit var input1: EditText
    private lateinit var input2: EditText
    private lateinit var errorText: TextView
    private lateinit var errorRow: LinearLayout
    private lateinit var saveButton: Button
    private lateinit var entryDots: List<View>
    private lateinit var confirmDots: List<View>
    private lateinit var activeInput: EditText
    private var suppressErrorClear = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_pin)

        val change = intent.getBooleanExtra(EXTRA_CHANGE, false)
        findViewById<TextView>(R.id.setPinTitle).text =
            getString(if (change) R.string.pin_change_title else R.string.pin_create_title)

        input1 = findViewById(R.id.pinInput1)
        input2 = findViewById(R.id.pinInput2)
        errorText = findViewById(R.id.setPinError)
        errorRow = findViewById(R.id.setPinErrorRow)
        saveButton = findViewById(R.id.setPinSave)

        entryDots = listOf(
            findViewById(R.id.entryDot1), findViewById(R.id.entryDot2),
            findViewById(R.id.entryDot3), findViewById(R.id.entryDot4),
            findViewById(R.id.entryDot5), findViewById(R.id.entryDot6),
        )
        confirmDots = listOf(
            findViewById(R.id.confirmDot1), findViewById(R.id.confirmDot2),
            findViewById(R.id.confirmDot3), findViewById(R.id.confirmDot4),
            findViewById(R.id.confirmDot5), findViewById(R.id.confirmDot6),
        )
        activeInput = input1

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // user typed manually: clear the old error and re-evaluate
                if (!suppressErrorClear) {
                    errorText.visibility = View.GONE
                    errorRow.visibility = View.GONE
                }
                suppressErrorClear = false
                saveButton.isEnabled = input1.text.length >= 4 && input2.text.length >= 4
                saveButton.alpha = if (saveButton.isEnabled) 1f else 0.5f
                // entry row full → keypad input flows to confirmation
                if (input1.text.length >= MAX_PIN_LENGTH) activeInput = input2
                renderDots(entryDots, input1.text.length)
                renderDots(confirmDots, input2.text.length)
            }
        }
        input1.addTextChangedListener(watcher)
        input2.addTextChangedListener(watcher)
        renderDots(entryDots, 0)
        renderDots(confirmDots, 0)

        // tapping a dot row routes the keypad to that field
        findViewById<View>(R.id.entryDotsFrame).setOnClickListener { activeInput = input1 }
        findViewById<View>(R.id.confirmDotsFrame).setOnClickListener { activeInput = input2 }

        bindKeypad()
        findViewById<View>(R.id.setPinCancel).setOnClickListener { finish() }
        saveButton.setOnClickListener { save() }
    }

    /** Shared on-screen keypad: digits append to the active capture field. */
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
        keyViews.add(saveButton)
        UiAnim.pressable(*keyViews.toTypedArray())
    }

    private fun appendDigit(digit: Char) {
        val current = activeInput.text.toString()
        if (current.length >= MAX_PIN_LENGTH) return
        activeInput.setText(current + digit)
    }

    private fun deleteDigit() {
        val current = activeInput.text.toString()
        if (current.isEmpty()) return
        activeInput.setText(current.dropLast(1))
    }

    /** Same dot rule as the PIN gate: stroke → accent, row grows past four. */
    private fun renderDots(row: List<View>, length: Int) {
        row.forEachIndexed { i, dot ->
            // dots 5/6 only appear once the first four are filled (PINs are 4-6)
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

    private fun save() {
        val pin = input1.text.toString()
        val confirm = input2.text.toString()
        val err = PinManager.validate(pin)
        when {
            err != 0 -> {
                showErr(err)
                markRowError(entryDots, R.id.entryDotsRow)
            }
            pin != confirm -> {
                showErr(R.string.pin_err_mismatch)
                markRowError(confirmDots, R.id.confirmDotsRow)
            }
            else -> {
                PinManager.savePin(this, pin)
                getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("pin_enabled", true).apply()
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun showErr(res: Int) {
        errorText.text = getString(res)
        errorText.visibility = View.VISIBLE
        errorRow.visibility = View.VISIBLE
        // clearing the confirm field triggers the watcher — keep the error on screen
        suppressErrorClear = true
        input2.setText("")
        saveButton.isEnabled = false
        saveButton.alpha = 0.5f
    }

    /**
     * Clay dots + shake on the offending row. Called after showErr so the
     * watcher's clear-render runs first — the next keystroke re-renders
     * the row back to normal states.
     */
    private fun markRowError(row: List<View>, rowContainerId: Int) {
        // route the keypad to the offending row so recovery taps fix it
        activeInput = if (row === entryDots) input1 else input2
        row.forEach { it.setBackgroundResource(R.drawable.f6_pin_dot_error) }
        android.animation.ObjectAnimator.ofFloat(
            findViewById<View>(rowContainerId), "translationX",
            0f, -26f, 26f, -18f, 18f, -8f, 8f, 0f,
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
