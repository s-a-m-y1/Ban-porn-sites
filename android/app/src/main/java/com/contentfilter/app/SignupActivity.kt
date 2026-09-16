package com.contentfilter.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SignupActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        val name = findViewById<EditText>(R.id.inputName)
        val email = findViewById<EditText>(R.id.inputEmail)
        val phone = findViewById<EditText>(R.id.inputPhone)
        val pass = findViewById<EditText>(R.id.inputPassword)
        val confirm = findViewById<EditText>(R.id.inputConfirm)
        val btn = findViewById<Button>(R.id.btnSignup)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val error = findViewById<TextView>(R.id.errorText)
        val toLogin = findViewById<TextView>(R.id.linkLogin)

        toLogin.setOnClickListener { finish() }

        btn.setOnClickListener {
            val n = name.text.toString().trim()
            val e = email.text.toString().trim()
            val ph = phone.text.toString().trim()
            val p = pass.text.toString()
            val c = confirm.text.toString()
            if (n.length < 2) { error.text = getString(R.string.auth_err_name); return@setOnClickListener }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) { error.text = getString(R.string.auth_err_email); return@setOnClickListener }
            if (!ph.matches(Regex("""^\+?[0-9]{7,15}$"""))) { error.text = getString(R.string.auth_err_phone); return@setOnClickListener }
            if (p.length < 8) { error.text = getString(R.string.auth_err_password); return@setOnClickListener }
            if (p != c) { error.text = getString(R.string.auth_err_confirm); return@setOnClickListener }
            error.text = ""; progress.visibility = View.VISIBLE; btn.isEnabled = false
            lifecycleScope.launch {
                val res = AuthRepository.signup(this@SignupActivity, n, e, ph, p, c)
                progress.visibility = View.GONE; btn.isEnabled = true
                res.onSuccess { (user, token) ->
                    AuthRepository.saveSession(this@SignupActivity, token, user)
                    startActivity(Intent(this@SignupActivity, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                    finish()
                }.onFailure { error.text = it.message ?: getString(R.string.auth_err_generic) }
            }
        }
    }
}
