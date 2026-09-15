package com.contentfilter.app

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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

        // F3: one entrance pass down the screen's rhythm (header, then each
        // card). Re-call safe: UiAnim resets state on every invocation, so
        // re-created fragment views animate again cleanly.
        UiAnim.staggeredEntrance(
            view.findViewById(R.id.f3Header),
            view.findViewById(R.id.f3CardCategories),
            view.findViewById(R.id.f3CardSchedule),
            view.findViewById(R.id.f3CardAppLock),
        )
    }

    private fun setupCategories(view: View) {
        val enabled = prefs.getStringSet("enabled_cats", null)?.takeIf { it.isNotEmpty() }
            ?: setOf(Categories.PORN, Categories.GAMBLING)

        fun bind(row: View, switch: SwitchCompat, cat: String, label: String) {
            switch.contentDescription = label
            switch.isChecked = cat in enabled
            switch.setOnCheckedChangeListener { _, checked ->
                lifecycleScope.launch {
                    val ctx = requireContext()
                    BlocklistRepository(ctx).setCategoryEnabled(cat, checked)
                    // refresh the in-memory index so the running filter sees
                    // the change instantly — without a service restart
                    BlocklistIndex.load(ctx)
                    val current = prefs.getStringSet(
                        "enabled_cats", setOf(Categories.PORN, Categories.GAMBLING),
                    )!!.toMutableSet()
                    if (checked) current.add(cat) else current.remove(cat)
                    prefs.edit().putStringSet("enabled_cats", current).apply()
                }
            }
            UiAnim.pressable(row)
            row.setOnClickListener { switch.performClick() }
        }
        bind(view.findViewById(R.id.rowPorn), view.findViewById(R.id.switchPorn), Categories.PORN, getString(R.string.cat_porn))
        bind(view.findViewById(R.id.rowGambling), view.findViewById(R.id.switchGambling), Categories.GAMBLING, getString(R.string.cat_gambling))
        bind(view.findViewById(R.id.rowFakenews), view.findViewById(R.id.switchFakenews), Categories.FAKENEWS, getString(R.string.cat_fakenews))
        bind(view.findViewById(R.id.rowMalware), view.findViewById(R.id.switchMalware), Categories.MALWARE, getString(R.string.cat_malware))
    }

    private fun setupManual(view: View) {
        val manualCount = view.findViewById<TextView>(R.id.manualCount)
        fun refreshCount() {
            lifecycleScope.launch {
                val list = BlocklistRepository(requireContext()).listCustom()
                manualCount.text = if (list.isEmpty()) "+" else list.size.toString()
            }
        }
        refreshCount()

        view.findViewById<View>(R.id.rowManual).apply {
            UiAnim.pressable(this)
            contentDescription = getString(R.string.manual_block_desc)
            setOnClickListener { showManualActions(manualCount) }
            setOnLongClickListener {
                showCustomList(manualCount)
                true
            }
        }
    }

    private fun showManualActions(manualCount: TextView) {
        // P2-1: manual block/unblock requires PIN if enabled
        if (prefs.getBoolean("pin_enabled", false) && PinManager.isPinSet(requireContext())) {
            pendingManualAction = { showManualActionsInternal(manualCount) }
            startActivityForResult(PinActivity.intent(requireContext(), getString(R.string.enter_pin_reason)), 8)
            return
        }
        showManualActionsInternal(manualCount)
    }

    private var pendingManualAction: (() -> Unit)? = null

    private fun showManualActionsInternal(manualCount: TextView) {
        val actions = arrayOf(
            getString(R.string.add_domain),
            getString(R.string.manual_review),
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.manual_block)
            .setItems(actions) { _, which ->
                if (which == 0) showAddDialog(manualCount) else showCustomList(manualCount)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddDialog(manualCount: TextView) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.domain_hint)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_URI or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textDirection = View.TEXT_DIRECTION_LTR
            setSingleLine(true)
        }
        layout.addView(input)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_domain)
            .setView(layout)
            .setPositiveButton(R.string.add, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            input.requestFocus()
            dialog.window?.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE,
            )
            input.post {
                val imm = requireContext().getSystemService(InputMethodManager::class.java)
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val domain = input.text.toString()
                lifecycleScope.launch {
                    val repo = BlocklistRepository(requireContext())
                    if (repo.addCustom(domain)) {
                        // new manual domain: live-refresh the in-memory index
                        BlocklistIndex.load(requireContext())
                        val list = repo.listCustom()
                        manualCount.text = if (list.isEmpty()) "+" else list.size.toString()
                        Toast.makeText(
                            requireContext(), R.string.domain_added, Toast.LENGTH_SHORT,
                        ).show()
                        dialog.dismiss()
                    } else {
                        input.error = getString(R.string.invalid_domain)
                    }
                }
            }
            input.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                    true
                } else {
                    false
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
                                    BlocklistIndex.load(requireContext())
                                    val l = repo.listCustom()
                                    manualCount.text = if (l.isEmpty()) "+" else l.size.toString()
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
        scheduleSwitch.contentDescription = getString(R.string.schedule_title)

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
        view.findViewById<View>(R.id.rowSchedule).apply {
            UiAnim.pressable(this)
            setOnClickListener { scheduleSwitch.performClick() }
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
        UiAnim.pressable(btnStart, btnEnd)
        btnStart.setOnClickListener { timePicker("schedule_start") }
        btnEnd.setOnClickListener { timePicker("schedule_end") }

        refreshScheduleUi()
    }

    private fun setupAppLock(view: View) {
        val applockSwitch = view.findViewById<SwitchCompat>(R.id.applockSwitch)
        applockSwitch.contentDescription = getString(R.string.applock_title)
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
            if (checked && !Settings.canDrawOverlays(requireContext())) {
                // Android 14+ blocks activity launches from background services;
                // the "display over other apps" grant is the documented exemption
                // that lets the block screen appear reliably over a blocked app.
                applockSwitch.isChecked = false
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.applock_need_overlay)
                    .setPositiveButton(R.string.grant) { _, _ ->
                        startActivityForResult(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${requireContext().packageName}"),
                            ), 7,
                        )
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                return@setOnCheckedChangeListener
            }
            prefs.edit().putBoolean("applock_enabled", checked).apply()
            if (checked) {
                AppLockService.start(requireContext())
                showAppPicker()
            } else {
                AppLockService.stop(requireContext())
            }
        }

        view.findViewById<View>(R.id.rowApplock).apply {
            UiAnim.pressable(this)
            setOnClickListener {
                if (applockSwitch.isChecked) showAppPicker() else applockSwitch.performClick()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7) {
            // back from the overlay-permission screen: finish enabling app lock
            val view = view ?: return
            val applockSwitch = view.findViewById<SwitchCompat>(R.id.applockSwitch)
            if (Settings.canDrawOverlays(requireContext())) {
                applockSwitch.isChecked = true // triggers the enable path in the listener
            }
        }
        if (requestCode == 8 && resultCode == android.app.Activity.RESULT_OK) {
            pendingManualAction?.invoke()
            pendingManualAction = null
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
            .setPositiveButton(R.string.done, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
