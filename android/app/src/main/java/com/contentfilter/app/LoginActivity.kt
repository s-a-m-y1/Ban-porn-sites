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

class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val email = findViewById<EditText>(R.id.inputEmail)
        val pass = findViewById<EditText>(R.id.inputPassword)
        val btn = findViewById<Button>(R.id.btnLogin)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val error = findViewById<TextView>(R.id.errorText)
        val toSignup = findViewById<TextView>(R.id.linkSignup)

        toSignup.setOnClickListener { startActivity(Intent(this, SignupActivity::class.java)); finish() }

        btn.setOnClickListener {
            val e = email.text.toString().trim()
            val p = pass.text.toString()
            if (e.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) { error.text = getString(R.string.auth_err_email); return@setOnClickListener }
            if (p.length < 8) { error.text = getString(R.string.auth_err_password); return@setOnClickListener }
            error.text = ""; progress.visibility = View.VISIBLE; btn.isEnabled = false
            lifecycleScope.launch {
                val res = AuthRepository.login(this@LoginActivity, e, p)
                progress.visibility = View.GONE; btn.isEnabled = true
                res.onSuccess { (user, token) ->
                    AuthRepository.saveSession(this@LoginActivity, token, user)
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                    finish()
                }.onFailure { error.text = it.message ?: getString(R.string.auth_err_generic) }
            }
        }
    }
}
