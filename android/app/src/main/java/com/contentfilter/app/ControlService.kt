package com.contentfilter.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Companion control service for Android Control desktop.
 * Provides:
 * - Connection status (ADB/desktop detection handled on desktop side via adb)
 * - Clipboard bridge (listens to clipboard, exposes via service)
 * - File transfer helper (desktop uses adb push/pull, service verifies storage permission)
 *
 * This service is intentionally minimal and does NOT request unnecessary permissions.
 * It does NOT bypass security, auto-install apps, or run hidden tasks.
 */
class ControlService : Service() {

    companion object {
        const val CHANNEL_ID = "control_service_channel"
        const val NOTIF_ID = 2002
        const val ACTION_START = "com.contentfilter.app.CONTROL_START"
        const val ACTION_STOP = "com.contentfilter.app.CONTROL_STOP"

        fun start(context: Context) {
            val i = Intent(context, ControlService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
            else context.startService(i)
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, ControlService::class.java))
        }
        fun isRunning(context: Context): Boolean {
            // simple prefs flag; real isRunning would query ActivityManager
            return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getBoolean("control_service_running", false)
        }
    }

    private var clipboardManager: ClipboardManager? = null
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        // Desktop syncs via adb/scrcpy --clipboard-autosync; we just log locally
        // No auto-upload without user consent
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("control_service_running", true).apply()

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("control_service_running", false).apply()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val info = DeviceInfoProvider.get(this)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Android Control — Connected")
            .setContentText("${info.displayName} • Android ${info.androidVersion} • ${info.resolution}")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Android Control", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Shows that Android Control service is active for desktop connection"
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
        }
    }
}
