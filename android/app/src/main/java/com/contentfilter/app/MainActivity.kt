package com.contentfilter.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var powerButton: ImageButton
    private lateinit var powerHalo: View
    private lateinit var statusText: TextView
    private lateinit var statusSubtitle: TextView
    private lateinit var statusDot: View
    private lateinit var powerLabel: TextView
    private lateinit var blockedCountText: TextView
    private lateinit var statsText: TextView
    private lateinit var themeSwitch: SwitchCompat
    private var vpnActive = false
    private var blocklistCount = 0

    private val prefs by lazy { getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("lang", Locale.getDefault().language.takeIf { it == "ar" } ?: "en")
            ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        powerButton = findViewById(R.id.powerButton)
        powerHalo = findViewById(R.id.powerHalo)
        statusText = findViewById(R.id.statusText)
        statusSubtitle = findViewById(R.id.statusSubtitle)
        statusDot = findViewById(R.id.statusDot)
        powerLabel = findViewById(R.id.powerLabel)
        blockedCountText = findViewById(R.id.blockedCountText)
        statsText = findViewById(R.id.statsText)
        themeSwitch = findViewById(R.id.themeSwitch)

        // ---- Dark mode (in-app override; default follows system) ----
        val savedDark = prefs.getBoolean("dark", false)
        val uiMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        val darkDefault = uiMode == Configuration.UI_MODE_NIGHT_YES
        themeSwitch.isChecked = prefs.getBoolean("dark_set", false) && savedDark || darkDefault

        themeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("dark", checked).putBoolean("dark_set", true).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            )
        }

        // ---- Language toggle ----
        val langValue: TextView = findViewById<TextView>(R.id.langValue)
        val currentLang = prefs.getString("lang", "en") ?: "en"
        langValue.text = if (currentLang == "ar") getString(R.string.arabic) else getString(R.string.english)
        findViewById<View>(R.id.langRow).setOnClickListener {
            val newLang = if (currentLang == "ar") "en" else "ar"
            prefs.edit().putString("lang", newLang).apply()
            recreate()
        }

        // ---- Email contact ----
        findViewById<Button>(R.id.emailButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:sam858y@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "[Content Filter] Feedback")
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "sam858y@gmail.com", Toast.LENGTH_LONG).show()
            }
        }

        // ---- Stats ----
        lifecycleScope.launch {
            val repo = BlocklistRepository(this@MainActivity)
            repo.ensureInitialBlocklist()
            SyncWorker.schedule(this@MainActivity)
            blocklistCount = repo.count()
            animateCount(statsText, blocklistCount)
            blockedCountText.text = prefs.getInt("blocked_total", 0).toString()
        }

        powerButton.setOnClickListener {
            if (vpnActive) {
                FilterVpnService.stop(this)
                setUiState(false, animated = true)
                return@setOnClickListener
            }
            val prepare = VpnService.prepare(this)
            if (prepare != null) {
                startActivityForResult(prepare, 1)
            } else {
                onVpnApproved()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) onVpnApproved()
    }

    override fun onResume() {
        super.onResume()
        blockedCountText.text = prefs.getInt("blocked_total", 0).toString()
    }

    private fun onVpnApproved() {
        FilterVpnService.start(this)
        setUiState(true, animated = true)
    }

    private fun setUiState(active: Boolean, animated: Boolean) {
        vpnActive = active

        // button + halo crossfade colors
        powerButton.background = ContextCompat.getDrawable(
            this, if (active) R.drawable.btn_primary else R.drawable.btn_danger,
        )
        powerHalo.setBackgroundResource(
            if (active) R.drawable.power_bg_on else R.drawable.power_bg_off,
        )

        if (animated) {
            val punchX = ObjectAnimator.ofFloat(powerButton, "scaleX", 1f, 0.85f, 1.15f, 1f)
            val punchY = ObjectAnimator.ofFloat(powerButton, "scaleY", 1f, 0.85f, 1.15f, 1f)
            val haloPulse = ObjectAnimator.ofFloat(powerHalo, "alpha", 1f, 0.4f, 1f)
            AnimatorSet().apply {
                playTogether(punchX, punchY, haloPulse)
                duration = 450
                start()
            }
        }

        statusDot.setBackgroundResource(
            if (active) R.drawable.status_dot_on else R.drawable.status_dot_off,
        )
        if (active) {
            statusDot.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse))
        } else {
            statusDot.clearAnimation()
        }

        statusText.text = getString(if (active) R.string.status_on else R.string.status_off)
        statusSubtitle.text = getString(
            if (active) R.string.status_on_subtitle else R.string.status_off_subtitle,
        )
        powerLabel.text = getString(
            if (active) R.string.stop_blocking else R.string.start_blocking,
        )

        if (animated) {
            val fade = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            statusText.startAnimation(fade)
            statusSubtitle.startAnimation(fade)
        }
    }

    private fun animateCount(view: TextView, target: Int) {
        if (target <= 0) {
            view.text = "0"
            return
        }
        ValueAnimator.ofInt(0, target).apply {
            duration = 800
            addUpdateListener { view.text = (it.animatedValue as Int).toString() }
            start()
        }
    }
}
