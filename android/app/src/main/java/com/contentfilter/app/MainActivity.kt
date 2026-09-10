package com.contentfilter.app

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var manualCount: TextView
    private lateinit var reportCount: TextView
    private lateinit var reportBars: LinearLayout
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
        manualCount = findViewById(R.id.manualCount)
        reportCount = findViewById(R.id.reportCount)
        reportBars = findViewById(R.id.reportBars)

        setupTheme()
        setupLanguage()
        setupEmail()
        setupStats()
        setupCategorySwitches()
        setupManualBlock()
        setupSchedule()
        setupAppLock()
        setupSecurity()
        setupPower()

        // restore VPN state indicator
        vpnActive = prefs.getBoolean("vpn_running", false)
        if (vpnActive) setUiState(true, animated = false)

        // apply scheduled state on open
        ScheduleManager.apply(this)
    }

    // ---------- Theme ----------
    private fun setupTheme() {
        val savedDark = prefs.getBoolean("dark", false)
        val darkDefault = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        themeSwitch.isChecked = prefs.getBoolean("dark_set", false) && savedDark || darkDefault
        themeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("dark", checked).putBoolean("dark_set", true).apply()
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                if (checked) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            )
        }
    }

    // ---------- Language ----------
    private fun setupLanguage() {
        val langValue = findViewById<TextView>(R.id.langValue)
        val currentLang = prefs.getString("lang", "en") ?: "en"
        langValue.text = if (currentLang == "ar") getString(R.string.arabic)
        else getString(R.string.english)
        findViewById<View>(R.id.langRow).setOnClickListener {
            prefs.edit().putString("lang", if (currentLang == "ar") "en" else "ar").apply()
            recreate()
        }
    }

    // ---------- Email ----------
    private fun setupEmail() {
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
    }

    // ---------- Stats ----------
    private fun setupStats() {
        lifecycleScope.launch {
            val repo = BlocklistRepository(this@MainActivity)
            repo.ensureInitialBlocklist()
            SyncWorker.schedule(this@MainActivity)
            animateCount(statsText, repo.count())
            blockedCountText.text = prefs.getInt("blocked_total", 0).toString()
            loadWeeklyReport()
        }
    }

    // ---------- Category switches ----------
    private fun setupCategorySwitches() {
        val enabled = prefs.getStringSet("enabled_cats", setOf(Categories.PORN, Categories.MALWARE))!!
        val swPorn = findViewById<SwitchCompat>(R.id.switchPorn)
        val swGambling = findViewById<SwitchCompat>(R.id.switchGambling)
        val swFakenews = findViewById<SwitchCompat>(R.id.switchFakenews)
        val swMalware = findViewById<SwitchCompat>(R.id.switchMalware)

        swPorn.isChecked = Categories.PORN in enabled
        swGambling.isChecked = Categories.GAMBLING in enabled
        swFakenews.isChecked = Categories.FAKENEWS in enabled
        swMalware.isChecked = Categories.MALWARE in enabled

        fun bind(switch: SwitchCompat, cat: String) {
            switch.setOnCheckedChangeListener { _, checked ->
                lifecycleScope.launch {
                    BlocklistRepository(this@MainActivity).setCategoryEnabled(cat, checked)
                    val current = prefs.getStringSet("enabled_cats",
                        setOf(Categories.PORN, Categories.MALWARE))!!.toMutableSet()
                    if (checked) current.add(cat) else current.remove(cat)
                    prefs.edit().putStringSet("enabled_cats", current).apply()
                }
            }
        }
        bind(swPorn, Categories.PORN)
        bind(swGambling, Categories.GAMBLING)
        bind(swFakenews, Categories.FAKENEWS)
        bind(swMalware, Categories.MALWARE)
    }

    // ---------- Manual block ----------
    private fun setupManualBlock() {
        findViewById<View>(R.id.rowManual).setOnClickListener { showManualBlockDialog() }
        lifecycleScope.launch {
            val list = BlocklistRepository(this@MainActivity).listCustom()
            manualCount.text = if (list.isEmpty()) "" else list.size.toString()
        }
    }

    private fun showManualBlockDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }
        val input = EditText(this).apply { hint = getString(R.string.domain_hint) }
        layout.addView(input)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_domain)
            .setView(layout)
            .setPositiveButton(R.string.add, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val domain = input.text.toString()
                lifecycleScope.launch {
                    val repo = BlocklistRepository(this@MainActivity)
                    if (repo.addCustom(domain)) {
                        val list = repo.listCustom()
                        manualCount.text = if (list.isEmpty()) "" else list.size.toString()
                        Toast.makeText(this@MainActivity, R.string.domain_added, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        input.error = getString(R.string.invalid_domain)
                    }
                }
            }
        }
        dialog.show()

        // also show existing list with remove buttons
        lifecycleScope.launch {
            val existing = BlocklistRepository(this@MainActivity).listCustom()
            if (existing.isNotEmpty()) {
                // (list shown in a follow-up dialog via long-press on the row)
            }
        }
        findViewById<View>(R.id.rowManual).setOnLongClickListener {
            showCustomListDialog()
            true
        }
    }

    private fun showCustomListDialog() {
        lifecycleScope.launch {
            val list = BlocklistRepository(this@MainActivity).listCustom()
            val names = list.map { it.domain }
            val items = if (names.isEmpty()) arrayOf(getString(R.string.empty_custom)) else names.toTypedArray()
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.manual_block)
                .setItems(items) { _, which ->
                    if (names.isNotEmpty()) {
                        val domain = names[which]
                        AlertDialog.Builder(this@MainActivity)
                            .setMessage(domain)
                            .setPositiveButton(R.string.remove) { _, _ ->
                                lifecycleScope.launch {
                                    val repo = BlocklistRepository(this@MainActivity)
                                    repo.removeCustom(domain)
                                    val l = repo.listCustom()
                                    manualCount.text = if (l.isEmpty()) "" else l.size.toString()
                                }
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    // ---------- Schedule ----------
    private fun setupSchedule() {
        val scheduleSwitch = findViewById<SwitchCompat>(R.id.scheduleSwitch)
        val timesRow = findViewById<LinearLayout>(R.id.scheduleTimesRow)
        val btnStart = findViewById<Button>(R.id.btnStartTime)
        val btnEnd = findViewById<Button>(R.id.btnEndTime)
        val scheduleValue = findViewById<TextView>(R.id.scheduleValue)

        fun refreshScheduleUi() {
            val enabled = prefs.getBoolean("schedule_enabled", false)
            scheduleSwitch.isChecked = enabled
            timesRow.visibility = if (enabled) View.VISIBLE else View.GONE
            if (enabled) {
                val s = prefs.getInt("schedule_start", 480)
                val e = prefs.getInt("schedule_end", 1380)
                scheduleValue.text = getString(
                    R.string.schedule_next,
                    String.format(Locale.US, "%02d:%02d", s / 60, s % 60),
                    String.format(Locale.US, "%02d:%02d", e / 60, e % 60),
                )
            } else {
                scheduleValue.text = getString(R.string.schedule_desc)
            }
        }

        scheduleSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("schedule_enabled", checked).apply()
            refreshScheduleUi()
            if (checked) ScheduleManager.setAlarms(this)
            ScheduleManager.apply(this)
        }

        fun timePicker(prefsKey: String) {
            val current = prefs.getInt(prefsKey, if (prefsKey == "schedule_start") 480 else 1380)
            TimePickerDialog(this, { _, hour, minute ->
                prefs.edit().putInt(prefsKey, hour * 60 + minute).apply()
                refreshScheduleUi()
                if (prefs.getBoolean("schedule_enabled", false)) ScheduleManager.setAlarms(this)
            }, current / 60, current % 60, true).show()
        }
        btnStart.setOnClickListener { timePicker("schedule_start") }
        btnEnd.setOnClickListener { timePicker("schedule_end") }

        refreshScheduleUi()
    }

    // ---------- App lock ----------
    private fun setupAppLock() {
        val applockSwitch = findViewById<SwitchCompat>(R.id.applockSwitch)
        val applockDesc = findViewById<TextView>(R.id.applockDesc)
        applockSwitch.isChecked = prefs.getBoolean("applock_enabled", false)

        applockSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked && !hasUsageAccess()) {
                applockSwitch.isChecked = false
                AlertDialog.Builder(this)
                    .setMessage(R.string.applock_need_permission)
                    .setPositiveButton(R.string.grant) { _, _ ->
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                return@setOnCheckedChangeListener
            }
            prefs.edit().putBoolean("applock_enabled", checked).apply()
            if (checked) {
                AppLockService.start(this)
                applockDesc.text = getString(R.string.applock_desc)
            } else {
                AppLockService.stop(this)
            }
        }

        findViewById<View>(R.id.rowApplock).setOnClickListener {
            if (applockSwitch.isChecked) showAppPicker()
        }
    }

    private fun hasUsageAccess(): Boolean {
        try {
            val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(), packageName,
            )
            return mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            return false
        }
    }

    private fun showAppPicker() {
        val installed = packageManager.getInstalledApplications(0)
            .filter {
                it.packageName != packageName &&
                    it.packageName != "android" &&
                    packageManager.getLaunchIntentForPackage(it.packageName) != null
            }
            .map { it.packageName to packageManager.getApplicationLabel(it).toString() }
            .sortedBy { it.second }

        val blocked = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
        val checked = installed.map { it.first in blocked }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.applock_title)
            .setMultiChoiceItems(
                installed.map { it.second }.toTypedArray(), checked,
            ) { _, which, isChecked ->
                val pkg = installed[which].first
                val current = prefs.getStringSet("blocked_apps", emptySet())!!.toMutableSet()
                if (isChecked) current.add(pkg) else current.remove(pkg)
                prefs.edit().putStringSet("blocked_apps", current).apply()
            }
            .setPositiveButton(R.string.pin_confirm, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------- Security (PIN + admin + persistence) ----------
    private fun setupSecurity() {
        val pinSwitch = findViewById<SwitchCompat>(R.id.pinSwitch)
        val adminSwitch = findViewById<SwitchCompat>(R.id.adminSwitch)
        val autostartSwitch = findViewById<SwitchCompat>(R.id.autostartSwitch)
        val alwaysOnSwitch = findViewById<SwitchCompat>(R.id.alwaysOnSwitch)

        pinSwitch.isChecked = prefs.getBoolean("pin_enabled", false)
        adminSwitch.isChecked = prefs.getBoolean("admin_enabled", false)
        autostartSwitch.isChecked = prefs.getBoolean("autostart", true)

        // check if we're already the system always-on VPN
        val alwaysOn = try {
            val conn = getSystemService(ConnectivityManager::class.java)
            val m = android.net.ConnectivityManager::class.java
                .getMethod("getAlwaysOnVpnPackageForUser", Int::class.javaPrimitiveType)
            m.invoke(conn, android.os.Process.myUid() / 100000) == packageName
        } catch (_: Exception) {
            false
        }
        alwaysOnSwitch.isChecked = alwaysOn
        prefs.edit().putBoolean("always_on", alwaysOn).apply()

        // autostart on boot
        autostartSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("autostart", checked).apply()
        }

        // always-on VPN: deep-link to system VPN settings
        alwaysOnSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.always_on_title)
                    .setMessage(R.string.always_on_ask)
                    .setPositiveButton(R.string.enable) { _, _ ->
                        try {
                            startActivity(Intent("android.net.vpn.SETTINGS"))
                        } catch (_: Exception) {
                            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                        }
                    }
                    .setNegativeButton(R.string.later) { _, _ ->
                        alwaysOnSwitch.isChecked = false
                    }
                    .show()
            }
        }

        pinSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                val input = EditText(this).apply {
                    hint = getString(R.string.pin_enter_new)
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(android.text.InputFilter.LengthFilter(6))
                }
                AlertDialog.Builder(this)
                    .setTitle(R.string.pin_lock)
                    .setView(input)
                    .setPositiveButton(R.string.pin_confirm) { _, _ ->
                        val pin = input.text.toString()
                        if (pin.length >= 4) {
                            prefs.edit().putString("pin", pin).putBoolean("pin_enabled", true).apply()
                        } else {
                            pinSwitch.isChecked = false
                            Toast.makeText(this, R.string.invalid_domain, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> pinSwitch.isChecked = false }
                    .show()
            } else {
                prefs.edit().putBoolean("pin_enabled", false).apply()
            }
        }

        adminSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(
                        android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        android.content.ComponentName(this@MainActivity, AdminReceiver::class.java),
                    )
                    putExtra(
                        android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.uninstall_protect_desc),
                    )
                }
                startActivityForResult(intent, 2)
            } else {
                val dpm = getSystemService(android.app.admin.DevicePolicyManager::class.java)
                dpm.removeActiveAdmin(android.content.ComponentName(this@MainActivity, AdminReceiver::class.java))
                prefs.edit().putBoolean("admin_enabled", false).apply()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1 -> if (resultCode == RESULT_OK) onVpnApproved()
            2 -> {
                val dpm = getSystemService(android.app.admin.DevicePolicyManager::class.java)
                val active = dpm.isAdminActive(android.content.ComponentName(this@MainActivity, AdminReceiver::class.java))
                prefs.edit().putBoolean("admin_enabled", active).apply()
                findViewById<SwitchCompat>(R.id.adminSwitch).isChecked = active
            }
        }
    }

    // ---------- Power ----------
    private fun setupPower() {
        powerButton.setOnClickListener {
            if (vpnActive) {
                if (prefs.getBoolean("pin_enabled", false)) {
                    val intent = Intent(this, PinActivity::class.java)
                    startActivityForResult(intent, 3)
                } else {
                    FilterVpnService.stop(this)
                    prefs.edit().putBoolean("vpn_running", false).apply()
                    setUiState(false, animated = true)
                }
                return@setOnClickListener
            }
            val prepare = VpnService.prepare(this)
            if (prepare != null) startActivityForResult(prepare, 1) else onVpnApproved()
        }
    }

    private fun onVpnApproved() {
        FilterVpnService.start(this)
        prefs.edit().putBoolean("vpn_running", true).apply()
        setUiState(true, animated = true)
    }

    override fun onResume() {
        super.onResume()
        blockedCountText.text = prefs.getInt("blocked_total", 0).toString()
        lifecycleScope.launch { loadWeeklyReport() }
    }

    // ---------- Weekly report + heatmap ----------
    private suspend fun loadWeeklyReport() {
        val repo = BlocklistRepository(this)
        val stats = repo.dailyStats(7)
        val total = stats.sumOf { it.blockedCount }
        reportCount.text = total.toString()

        // bar chart (7 days)
        reportBars.removeAllViews()
        val max = (stats.maxOfOrNull { it.blockedCount } ?: 0).coerceAtLeast(1)
        val days = stats.reversed()
        if (stats.isNotEmpty()) {
            for (day in days) {
                val bar = LayoutInflater.from(this)
                    .inflate(R.layout.report_bar, reportBars, false) as LinearLayout
                val fill = bar.findViewById<View>(R.id.barFill)
                val label = bar.findViewById<TextView>(R.id.barLabel)
                val h = (day.blockedCount * 100 / max).coerceAtLeast(4)
                fill.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, h * 3,
                )
                label.text = day.day.substring(8)
                reportBars.addView(bar)
            }
        }

        // GitHub-style heatmap (105 days)
        val all = repo.allStats().toMap()
        val heatmap = findViewById<HeatmapView>(R.id.heatmap)
        val isDark = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        heatmap.setData(all, isDark)
        heatmap.onCellClick { day, count ->
            findViewById<TextView>(R.id.heatmapSelected).text =
                getString(R.string.heatmap_day_fmt, day, count)
        }
    }

    // ---------- UI state ----------
    private fun setUiState(active: Boolean, animated: Boolean) {
        vpnActive = active
        powerButton.background = ContextCompat.getDrawable(
            this, if (active) R.drawable.btn_primary else R.drawable.btn_danger,
        )
        powerHalo.setBackgroundResource(
            if (active) R.drawable.power_bg_on else R.drawable.power_bg_off,
        )
        if (animated) {
            val punchX = android.animation.ObjectAnimator.ofFloat(powerButton, "scaleX", 1f, 0.85f, 1.15f, 1f)
            val punchY = android.animation.ObjectAnimator.ofFloat(powerButton, "scaleY", 1f, 0.85f, 1.15f, 1f)
            android.animation.AnimatorSet().apply {
                playTogether(punchX, punchY)
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
    }

    private fun animateCount(view: TextView, target: Int) {
        if (target <= 0) {
            view.text = "0"
            return
        }
        android.animation.ValueAnimator.ofInt(0, target).apply {
            duration = 800
            addUpdateListener { view.text = (it.animatedValue as Int).toString() }
            start()
        }
    }
}
