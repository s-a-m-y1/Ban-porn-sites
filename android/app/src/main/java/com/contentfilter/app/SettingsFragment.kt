package com.contentfilter.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import java.util.Locale

class SettingsFragment : Fragment() {

    private val prefs by lazy { requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLanguage(view)
        setupTheme(view)
        setupSecurity(view)
    }

    private fun setupLanguage(view: View) {
        val langValue = view.findViewById<TextView>(R.id.langValue)
        val currentLang = prefs.getString("lang", "en") ?: "en"
        langValue.text = if (currentLang == "ar") getString(R.string.arabic)
        else getString(R.string.english)
        view.findViewById<View>(R.id.langRow).setOnClickListener {
            prefs.edit().putString("lang", if (currentLang == "ar") "en" else "ar").apply()
            requireActivity().recreate()
        }
    }

    private fun setupTheme(view: View) {
        val themeSwitch = view.findViewById<SwitchCompat>(R.id.themeSwitch)
        val mode = prefs.getString("theme_mode", "system") ?: "system"
        themeSwitch.isChecked = mode == "dark"
        themeSwitch.setOnCheckedChangeListener { _, checked ->
            val newMode = if (checked) "dark" else "light"
            prefs.edit().putString("theme_mode", newMode).apply()
            AppCompatDelegate.setDefaultNightMode(
                when (newMode) {
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                },
            )
        }
    }

    private fun setupSecurity(view: View) {
        val pinSwitch = view.findViewById<SwitchCompat>(R.id.pinSwitch)
        val adminSwitch = view.findViewById<SwitchCompat>(R.id.adminSwitch)
        val autostartSwitch = view.findViewById<SwitchCompat>(R.id.autostartSwitch)
        val alwaysOnSwitch = view.findViewById<SwitchCompat>(R.id.alwaysOnSwitch)

        pinSwitch.isChecked = prefs.getBoolean("pin_enabled", false)
        adminSwitch.isChecked = prefs.getBoolean("admin_enabled", false)
        autostartSwitch.isChecked = prefs.getBoolean("autostart", true)

        val alwaysOn = try {
            val conn = requireContext().getSystemService(ConnectivityManager::class.java)
            val m = ConnectivityManager::class.java
                .getMethod("getAlwaysOnVpnPackageForUser", Int::class.javaPrimitiveType)
            m.invoke(conn, android.os.Process.myUid() / 100000) == requireContext().packageName
        } catch (_: Exception) {
            false
        }
        alwaysOnSwitch.isChecked = alwaysOn
        prefs.edit().putBoolean("always_on", alwaysOn).apply()

        autostartSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("autostart", checked).apply()
        }

        alwaysOnSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                AlertDialog.Builder(requireContext())
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
                val input = EditText(requireContext()).apply {
                    hint = getString(R.string.pin_enter_new)
                    inputType = InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(android.text.InputFilter.LengthFilter(6))
                }
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.pin_lock)
                    .setView(input)
                    .setPositiveButton(R.string.pin_confirm) { _, _ ->
                        val pin = input.text.toString()
                        if (pin.length >= 4) {
                            prefs.edit().putString("pin", pin).putBoolean("pin_enabled", true).apply()
                        } else {
                            pinSwitch.isChecked = false
                            Toast.makeText(
                                requireContext(), R.string.invalid_domain, Toast.LENGTH_SHORT,
                            ).show()
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
                val intent = Intent(
                    android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN,
                ).apply {
                    putExtra(
                        android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        android.content.ComponentName(requireContext(), AdminReceiver::class.java),
                    )
                    putExtra(
                        android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.uninstall_protect_desc),
                    )
                }
                startActivityForResult(intent, 2)
            } else {
                val dpm = requireContext().getSystemService(
                    android.app.admin.DevicePolicyManager::class.java,
                )
                dpm.removeActiveAdmin(
                    android.content.ComponentName(requireContext(), AdminReceiver::class.java),
                )
                prefs.edit().putBoolean("admin_enabled", false).apply()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            val dpm = requireContext().getSystemService(
                android.app.admin.DevicePolicyManager::class.java,
            )
            val active = dpm.isAdminActive(
                android.content.ComponentName(requireContext(), AdminReceiver::class.java),
            )
            prefs.edit().putBoolean("admin_enabled", active).apply()
            view?.findViewById<SwitchCompat>(R.id.adminSwitch)?.isChecked = active
        }
    }
}
