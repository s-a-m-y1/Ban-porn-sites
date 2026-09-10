package com.contentfilter.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FilterVpnService : VpnService() {

    companion object {
        const val TAG = "FilterVpnService"
        const val CHANNEL_ID = "content_filter_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.contentfilter.app.STOP_VPN"
        const val UPSTREAM_DNS = "8.8.8.8"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FilterVpnService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, FilterVpnService::class.java).apply {
                    putExtra("action", ACTION_STOP)
                },
            )
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dnsParser: DnsPacketParser
    private lateinit var blockedDomainDao: BlockedDomainDao
    private var enabledCategories: List<String> = listOf(Categories.PORN, Categories.MALWARE)
    private var upstreamSocket: DatagramSocket? = null
    private var blockedCount = 0

    override fun onCreate() {
        super.onCreate()
        dnsParser = DnsPacketParser()
        blockedDomainDao = BlocklistDatabase.getInstance(this).blockedDomainDao()
        enabledCategories = Categories.enabled(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getStringExtra("action") == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        startForeground(NOTIFICATION_ID, buildNotification("Filtering active"))
        try {
            vpnInterface = Builder()
                .setSession("CF")
                .addAddress("10.111.0.1", 32)
                .addDnsServer("10.111.0.2")
                .addRoute("10.111.0.2", 32)
                .setMtu(1500)
                .establish() ?: run {
                    stopSelf()
                    return
                }
            upstreamSocket = DatagramSocket().apply { soTimeout = 5000 }
            protect(upstreamSocket) // bypass VPN, go straight to network
            running.set(true)
            scope.launch { pumpPackets() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            stopSelf()
        }
    }

    private fun stopVpn() {
        running.set(false)
        upstreamSocket?.close()
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        scope.cancel()
    }

    override fun onDestroy() {
        running.set(false)
        upstreamSocket?.close()
        vpnInterface?.close()
        super.onDestroy()
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(
            NOTIFICATION_ID,
            buildNotification("Filtering active - $blockedCount blocked"),
        )
    }

    private suspend fun pumpPackets() {
        val iface = vpnInterface ?: return
        val input = FileInputStream(iface.fileDescriptor)
        val output = FileOutputStream(iface.fileDescriptor)

        val buffer = ByteArray(32767)
        while (running.get()) {
            val length = try {
                input.read(buffer)
            } catch (_: Exception) {
                break
            }
            if (length <= 0) continue
            val packet = buffer.copyOf(length)

            if (dnsParser.isDnsPacket(packet)) {
                val domain = dnsParser.extractQueryDomain(packet)
                if (domain != null) {
                    if (isBlocked(domain)) {
                        blockedCount++
                        if (blockedCount % 10 == 0) updateNotification()
                        scope.launch { StatsTracker.recordBlocked(this@FilterVpnService, domain) }
                        Log.i(TAG, "Blocked: $domain")
                        val response = dnsParser.buildNxDomainResponse(packet)
                        output.write(response)
                    } else {
                        Log.d(TAG, "Allowed: $domain")
                        val answer = resolveUpstream(packet)
                        if (answer != null) {
                            output.write(
                                dnsParser.buildDnsResponse(packet, answer),
                            )
                        }
                    }
                    continue
                }
            }
            // Non-DNS traffic: nothing else is routed through the TUN,
            // so this should rarely happen — pass it through unchanged.
            output.write(packet)
        }
    }

    /** Forward the raw DNS query to the upstream resolver and return the raw answer. */
    private fun resolveUpstream(queryPacket: ByteArray): ByteArray? {
        val socket = upstreamSocket ?: return null
        return try {
            val payload = dnsParser.dnsPayload(queryPacket)
            val outPacket = DatagramPacket(
                payload, payload.size,
                InetAddress.getByName(UPSTREAM_DNS), 53,
            )
            socket.send(outPacket)
            val answerBuf = ByteArray(4096)
            val answerPacket = DatagramPacket(answerBuf, answerBuf.size)
            socket.receive(answerPacket)
            answerBuf.copyOf(answerPacket.length)
        } catch (e: Exception) {
            Log.w(TAG, "Upstream resolve failed: ${e.message}")
            null
        }
    }

    private suspend fun isBlocked(domain: String): Boolean {
        // reload enabled categories on each check window (cheap: SharedPreferences read)
        val cats = Categories.enabled(this)
        var d = domain.trimEnd('.')
        while (d.contains('.')) {
            if (blockedDomainDao.isCustomBlocked(d) > 0) return true
            if (cats.isNotEmpty() && blockedDomainDao.isBlockedIn(d, cats) > 0) return true
            d = d.substringAfter('.')
        }
        return false
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Content Filter")
            .setContentText(text)
            // fully transparent icon: nothing appears in the status bar
            .setSmallIcon(R.drawable.ic_notif_transparent)
            .setOngoing(true)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(openIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Content Filter",
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = getString(R.string.channel_desc)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            setShowBadge(false)
            // no banner, no heads-up, silently sitting at the very bottom
            setImportance(NotificationManager.IMPORTANCE_MIN)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
