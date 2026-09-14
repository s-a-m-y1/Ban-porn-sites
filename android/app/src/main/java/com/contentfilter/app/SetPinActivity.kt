package com.contentfilter.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Create / change the app PIN: entry + confirmation with live validation.
 */
class SetPinActivity : BaseActivity() {

    companion object {
        const val EXTRA_CHANGE = "change"
        fun intent(context: Context, change: Boolean = false) =
            Intent(context, SetPinActivity::class.java).apply { putExtra(EXTRA_CHANGE, change) }
    }

    private lateinit var input1: EditText
    private lateinit var input2: EditText
    private lateinit var errorText: TextView
    private lateinit var saveButton: Button
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
        saveButton = findViewById(R.id.setPinSave)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // user typed manually: clear the old error and re-evaluate
                if (!suppressErrorClear) errorText.visibility = View.GONE
                suppressErrorClear = false
                saveButton.isEnabled = input1.text.length >= 4 && input2.text.length >= 4
                saveButton.alpha = if (saveButton.isEnabled) 1f else 0.5f
            }
        }
        input1.addTextChangedListener(watcher)
        input2.addTextChangedListener(watcher)
        input1.requestFocus()
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        input1.post {
            val imm = getSystemService(InputMethodManager::class.java)
            imm.showSoftInput(input1, InputMethodManager.SHOW_IMPLICIT)
        }

        findViewById<View>(R.id.setPinCancel).setOnClickListener { finish() }
        saveButton.setOnClickListener { save() }
    }

    private fun save() {
        val pin = input1.text.toString()
        val confirm = input2.text.toString()
        val err = PinManager.validate(pin)
        when {
            err != 0 -> showErr(err)
            pin != confirm -> showErr(R.string.pin_err_mismatch)
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
        // clearing the confirm field triggers the watcher — keep the error on screen
        suppressErrorClear = true
        input2.setText("")
        saveButton.isEnabled = false
        saveButton.alpha = 0.5f
    }
}
