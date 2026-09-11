package com.contentfilter.app

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class FeaturesFragment : Fragment() {

    private val prefs by lazy { requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_features, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategories(view)
        setupManual(view)
        setupSchedule(view)
        setupAppLock(view)
    }

    private fun setupCategories(view: View) {
        val enabled = prefs.getStringSet("enabled_cats", setOf(Categories.PORN, Categories.MALWARE))!!

        fun bind(switch: SwitchCompat, cat: String) {
            switch.isChecked = cat in enabled
            switch.setOnCheckedChangeListener { _, checked ->
                lifecycleScope.launch {
                    BlocklistRepository(requireContext()).setCategoryEnabled(cat, checked)
                    val current = prefs.getStringSet(
                        "enabled_cats", setOf(Categories.PORN, Categories.MALWARE),
                    )!!.toMutableSet()
                    if (checked) current.add(cat) else current.remove(cat)
                    prefs.edit().putStringSet("enabled_cats", current).apply()
                }
            }
        }
        bind(view.findViewById(R.id.switchPorn), Categories.PORN)
        bind(view.findViewById(R.id.switchGambling), Categories.GAMBLING)
        bind(view.findViewById(R.id.switchFakenews), Categories.FAKENEWS)
        bind(view.findViewById(R.id.switchMalware), Categories.MALWARE)
    }

    private fun setupManual(view: View) {
        val manualCount = view.findViewById<TextView>(R.id.manualCount)
        fun refreshCount() {
            lifecycleScope.launch {
                val list = BlocklistRepository(requireContext()).listCustom()
                manualCount.text = if (list.isEmpty()) "" else list.size.toString()
            }
        }
        refreshCount()

        view.findViewById<View>(R.id.rowManual).apply {
            setOnClickListener { showAddDialog(manualCount) }
            setOnLongClickListener {
                showCustomList(manualCount)
                true
            }
        }
    }

    private fun showAddDialog(manualCount: TextView) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }
        val input = EditText(requireContext()).apply { hint = getString(R.string.domain_hint) }
        layout.addView(input)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_domain)
            .setView(layout)
            .setPositiveButton(R.string.add, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val domain = input.text.toString()
                lifecycleScope.launch {
                    val repo = BlocklistRepository(requireContext())
                    if (repo.addCustom(domain)) {
                        val list = repo.listCustom()
                        manualCount.text = if (list.isEmpty()) "" else list.size.toString()
                        Toast.makeText(
                            requireContext(), R.string.domain_added, Toast.LENGTH_SHORT,
                        ).show()
                        dialog.dismiss()
                    } else {
                        input.error = getString(R.string.invalid_domain)
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showCustomList(manualCount: TextView) {
        lifecycleScope.launch {
            val list = BlocklistRepository(requireContext()).listCustom()
            val names = list.map { it.domain }
            val items = if (names.isEmpty()) arrayOf(getString(R.string.empty_custom))
            else names.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.manual_block)
                .setItems(items) { _, which ->
                    if (names.isNotEmpty()) {
                        val domain = names[which]
                        AlertDialog.Builder(requireContext())
                            .setMessage(domain)
                            .setPositiveButton(R.string.remove) { _, _ ->
                                lifecycleScope.launch {
                                    val repo = BlocklistRepository(requireContext())
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

    private fun setupSchedule(view: View) {
        val scheduleSwitch = view.findViewById<SwitchCompat>(R.id.scheduleSwitch)
        val timesRow = view.findViewById<LinearLayout>(R.id.scheduleTimesRow)
        val btnStart = view.findViewById<Button>(R.id.btnStartTime)
        val btnEnd = view.findViewById<Button>(R.id.btnEndTime)
        val scheduleValue = view.findViewById<TextView>(R.id.scheduleValue)

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
            if (checked) ScheduleManager.setAlarms(requireContext())
            ScheduleManager.apply(requireContext())
        }

        fun timePicker(prefsKey: String) {
            val current = prefs.getInt(prefsKey, if (prefsKey == "schedule_start") 480 else 1380)
            TimePickerDialog(requireContext(), { _, hour, minute ->
                prefs.edit().putInt(prefsKey, hour * 60 + minute).apply()
                refreshScheduleUi()
                if (prefs.getBoolean("schedule_enabled", false)) {
                    ScheduleManager.setAlarms(requireContext())
                }
            }, current / 60, current % 60, true).show()
        }
        btnStart.setOnClickListener { timePicker("schedule_start") }
        btnEnd.setOnClickListener { timePicker("schedule_end") }

        refreshScheduleUi()
    }

    private fun setupAppLock(view: View) {
        val applockSwitch = view.findViewById<SwitchCompat>(R.id.applockSwitch)
        applockSwitch.isChecked = prefs.getBoolean("applock_enabled", false)

        applockSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked && !hasUsageAccess()) {
                applockSwitch.isChecked = false
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.applock_need_permission)
                    .setPositiveButton(R.string.grant) { _, _ ->
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                return@setOnCheckedChangeListener
            }
            prefs.edit().putBoolean("applock_enabled", checked).apply()
            if (checked) AppLockService.start(requireContext())
            else AppLockService.stop(requireContext())
        }

        view.findViewById<View>(R.id.rowApplock).setOnClickListener {
            if (applockSwitch.isChecked) showAppPicker()
        }
    }

    private fun hasUsageAccess(): Boolean = try {
        val appOps = requireContext().getSystemService(
            android.app.Activity.APP_OPS_SERVICE,
        ) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(), requireContext().packageName,
        )
        mode == android.app.AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        false
    }

    private fun showAppPicker() {
        val installed = requireContext().packageManager.getInstalledApplications(0)
            .filter {
                it.packageName != requireContext().packageName &&
                    it.packageName != "android" &&
                    requireContext().packageManager.getLaunchIntentForPackage(it.packageName) != null
            }
            .map { it.packageName to requireContext().packageManager.getApplicationLabel(it).toString() }
            .sortedBy { it.second }

        val blocked = prefs.getStringSet("blocked_apps", emptySet())!!
        val checked = installed.map { it.first in blocked }.toBooleanArray()

        AlertDialog.Builder(requireContext())
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
}
