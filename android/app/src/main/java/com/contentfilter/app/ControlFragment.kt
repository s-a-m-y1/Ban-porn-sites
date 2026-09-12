package com.contentfilter.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class ControlFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val info = DeviceInfoProvider.get(requireContext())
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        view.findViewById<TextView>(R.id.controlConnectionStatus)?.apply {
            val running = ControlService.isRunning(requireContext())
            text = if (running) "● Desktop connected" else "○ Waiting for desktop"
            setTextColor(
                if (running) resources.getColor(R.color.teal, null) else resources.getColor(R.color.text_secondary, null)
            )
        }

        view.findViewById<TextView>(R.id.controlDeviceName)?.text = info.displayName
        view.findViewById<TextView>(R.id.controlAndroidVersion)?.text = "Android ${info.androidVersion} • API ${info.apiLevel}"
        view.findViewById<TextView>(R.id.controlResolution)?.text = info.resolution
        view.findViewById<TextView>(R.id.controlBattery)?.text = if (info.batteryLevel >= 0) "${info.batteryLevel}% Battery" else "Battery —"
        view.findViewById<TextView>(R.id.controlSerial)?.text = info.serial

        val serviceStatus = view.findViewById<TextView>(R.id.controlServiceStatus)
        val clipboardStatus = view.findViewById<TextView>(R.id.controlClipboardStatus)
        val fileStatus = view.findViewById<TextView>(R.id.controlFileStatus)

        fun refreshServices() {
            val running = ControlService.isRunning(requireContext())
            serviceStatus.text = if (running) "✓ Connection Service" else "○ Connection Service"
            clipboardStatus.text = "✓ Clipboard" // clipboard is always available via system
            fileStatus.text = "✓ File Transfer (via ADB push/pull)"
        }
        refreshServices()

        view.findViewById<Button>(R.id.btnToggleService)?.apply {
            val running = ControlService.isRunning(requireContext())
            text = if (running) "Stop Service" else "Start Service"
            setOnClickListener {
                if (ControlService.isRunning(requireContext())) {
                    ControlService.stop(requireContext())
                    Toast.makeText(requireContext(), "Service stopped", Toast.LENGTH_SHORT).show()
                } else {
                    // POST_NOTIFICATIONS permission needed on Android 13+
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        val perm = android.Manifest.permission.POST_NOTIFICATIONS
                        if (requireContext().checkSelfPermission(perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(arrayOf(perm), 1001)
                            return@setOnClickListener
                        }
                    }
                    ControlService.start(requireContext())
                    Toast.makeText(requireContext(), "Service started", Toast.LENGTH_SHORT).show()
                }
                refreshServices()
                // update button text
                val r = ControlService.isRunning(requireContext())
                text = if (r) "Stop Service" else "Start Service"
                view.findViewById<TextView>(R.id.controlConnectionStatus)?.apply {
                    this.text = if (r) "● Desktop connected" else "○ Waiting for desktop"
                }
            }
        }

        // Clipboard test buttons
        view.findViewById<Button>(R.id.btnCopyTest)?.setOnClickListener {
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("test", "Hello from Android Control"))
            Toast.makeText(requireContext(), "Copied to clipboard (desktop can sync via scrcpy)", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Button>(R.id.btnPasteTest)?.setOnClickListener {
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val txt = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: "(empty)"
            Toast.makeText(requireContext(), "Clipboard: $txt", Toast.LENGTH_LONG).show()
        }

        // Settings shortcut
        view.findViewById<Button>(R.id.btnOpenSettings)?.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.fragmentContainer, SettingsFragment()).commit()
        }

        // Explain permissions
        view.findViewById<TextView>(R.id.controlPermissionInfo)?.text =
            "No extra permissions required. Desktop connects via ADB over USB.\n" +
            "USB Debugging must be enabled and authorized on this device."
    }
}
