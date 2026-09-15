package com.contentfilter.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.os.SystemClock
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FilterVpnService : VpnService() {

    companion object {
        const val TAG = "FilterVpnService"
        const val CHANNEL_ID = "content_filter_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.contentfilter.app.STOP_VPN"
        const val UPSTREAM_DNS = "8.8.8.8"

        /** true while the packet pump is live; lets the UI self-heal a dead service */
        @Volatile var isRunning: Boolean = false
            private set

        /** set when establish() fails due to missing consent; the UI re-asks the user */
        @Volatile var needsConsent: Boolean = false

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FilterVpnService::class.java))
        }

        fun stop(context: Context) {
            // startForegroundService is allowed from the background (e.g. the
            // ScheduleReceiver alarm) while startService throws there. The stop
            // path must call startForeground() first to honour the FGS contract —
            // done in onStartCommand. stopService alone would NOT tear the VPN
            // down: the system holds a binding on the VpnService.
            context.startForegroundService(
                Intent(context, FilterVpnService::class.java).apply {
                    putExtra("action", ACTION_STOP)
                },
            )
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dnsParser: DnsPacketParser
    private var blockedCount = 0

    // the packet pump runs on its own single thread — never the main thread,
    // never a coroutine hopping between dispatcher threads (packet ordering
    // and cache locality both benefit from thread pinning)
    private var pumpThread: Thread? = null

    // spiritual reminder Toasts: one per 10s max, cycling through the pool
    @Volatile private var lastToastAt = 0L
    private var toastIndex = 0

    // the اتقي الله screen: one per 60s max — a burst of blocked queries
    // (ads on one page) must not harass the user with a dozen overlays
    @Volatile private var lastBlockScreenAt = 0L

    // DNS cache: domain -> (raw answer, expiry)
    private val dnsCache = java.util.concurrent.ConcurrentHashMap<String, Pair<ByteArray, Long>>()
    private var cacheSweptAt = 0L

    // separate socket per worker — concurrent upstream queries without head-of-line blocking
    private var resolverPool = newResolverPool()

    private fun newResolverPool() = java.util.concurrent.Executors.newFixedThreadPool(4) { r ->
        Thread(r, "dns-resolver").apply { isDaemon = true }
    }

    override fun onCreate() {
        super.onCreate()
        dnsParser = DnsPacketParser()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getStringExtra("action") == ACTION_STOP) {
            // startForegroundService() was used, so the FGS contract requires
            // a startForeground() call before we may tear everything down
            startForeground(NOTIFICATION_ID, buildNotification("Filtering active"))
            stopVpn()
            return START_NOT_STICKY
        }
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        startForeground(NOTIFICATION_ID, buildNotification("Filtering active"))
        if (running.get()) return
        // a previous stopVpn() on this same instance may have torn down the
        // pump runtime — rebuild it, or the coroutine would silently no-op
        if (resolverPool.isShutdown) resolverPool = newResolverPool()
        if (!scope.isActive) scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            vpnInterface = Builder()
                .setSession("CF")
                .addAddress("10.111.0.1", 32)
                .addDnsServer("10.111.0.2")
                .addRoute("10.111.0.2", 32)
                .setMtu(1500)
                .establish() ?: run {
                    Log.e(TAG, "VPN establish() returned null — consent missing or revoked")
                    needsConsent = true
                    stopSelf()
                    return
                }
            Log.i(TAG, "VPN interface established, starting DNS pump")

            // C2: load the blocklist into an in-memory HashSet exactly once
            // per service start — after this, every DNS lookup is O(1) and
            // never touches Room, disk, or SharedPreferences again.
            needsConsent = false
            running.set(true)
            scope.launch {
                runCatching {
                    BlocklistIndex.load(this@FilterVpnService)
                }.onFailure {
                    Log.w(TAG, "Blocklist index load failed: ${it.message}")
                }
                if (!running.get()) {
                    return@launch
                }

                isRunning = true
                // C3: the pump is a dedicated daemon thread — zero work happens
                // on the main thread, so the UI never waits on packet processing.
                pumpThread = Thread(this@FilterVpnService::pumpPackets, "dns-pump").apply {
                    isDaemon = true
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            stopSelf()
        }
    }

    private fun stopVpn() {
        running.set(false)
        isRunning = false
        resolverPool.shutdownNow()
        vpnInterface?.close()
        vpnInterface = null
        pumpThread?.let { t -> runCatching { t.join(500) } }
        pumpThread = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        scope.cancel()
    }

    override fun onDestroy() {
        running.set(false)
        isRunning = false
        resolverPool.shutdownNow()
        vpnInterface?.close()
        vpnInterface = null
        pumpThread?.let { t -> runCatching { t.join(500) } }
        pumpThread = null
        scope.cancel()
        super.onDestroy()
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(
            NOTIFICATION_ID,
            buildNotification("Filtering active - $blockedCount blocked"),
        )
    }

    /**
     * The packet pump — the whole hot path. Design rules:
     *
     * C1 — DNS only: the TUN routes just the DNS server IP, so non-DNS
     *      packets are rare strays; they're written back verbatim with zero
     *      parsing, zero logging, zero inspection. HTTPS/SNI is never touched.
     *
     * C2 — O(1) lookups: [BlocklistIndex] holds HashSets in memory; a blocked
     *      check is a handful of set hits, sub-microsecond at any list size.
     *      No disk reads, no Room queries, no SharedPreferences per packet.
     *
     * C4 — minimal copying: non-DNS packets are written back from the read
     *      buffer immediately. DNS queries are copied exactly once because
     *      resolver threads process allowed queries asynchronously while the
     *      pump reuses the read buffer; the upstream send uses an offset into
     *      that packet, not a second payload buffer.
     */
    private fun pumpPackets() {
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

            if (!dnsParser.isDnsPacket(buffer, length)) {
                // Non-DNS traffic: written back verbatim from the read buffer
                // with no allocation, logging, or deeper inspection.
                synchronized(output) { output.write(buffer, 0, length) }
                continue
            }

            // the single DNS-query copy: the pump reuses this buffer on the
            // next read while resolver threads still hold this packet
            val packet = buffer.copyOf(length)
            if (dnsParser.isDnsPacket(packet)) {
                val domain = dnsParser.extractQueryDomain(packet)
                if (domain != null) {
                    if (BlocklistIndex.isBlocked(domain)) {
                        blockedCount++
                        if (blockedCount % 10 == 0) updateNotification()
                        scope.launch { StatsTracker.recordBlocked(this@FilterVpnService, domain) }
                        // P1-1: category-aware + adaptive — pass category + attemptCount to overlay
                        val category = BlocklistIndex.getCategory(domain)
                        val attempt = BlockAttemptTracker.incrementAndGet(this@FilterVpnService, category)
                        showBlockToast(category, attempt)
                        maybeShowBlockScreen(domain, category, attempt)
                        val response = dnsParser.buildNxDomainResponse(packet)
                        synchronized(output) { output.write(response) }
                    } else {
                        // fire-and-forget: never stall the reader on the network.
                        // 30 domains on one page resolve in parallel instead of in series.
                        resolverPool.execute {
                            val answer = resolveUpstream(packet, domain)
                            if (answer != null) {
                                val out = dnsParser.buildDnsResponse(packet, answer)
                                synchronized(output) { output.write(out) }
                            }
                        }
                    }
                    continue
                }
            }
        }
    }

    /** Forward the raw DNS query to the upstream resolver and return the raw answer. */
    private fun resolveUpstream(queryPacket: ByteArray, domain: String): ByteArray? {
        val dnsStart = dnsParser.dnsPayloadStart(queryPacket) ?: return null
        val dnsLength = queryPacket.size - dnsStart
        val now = SystemClock.elapsedRealtime()

            // cache hit? reply instantly — no network round-trip
        dnsCache[domain]?.let { (answer, expiry) ->
            if (now < expiry) {
                // rewrite the answer's ID to match this query
                if (dnsStart + 1 < queryPacket.size) {
                    val patched = answer.copyOf()
                    patched[0] = queryPacket[dnsStart]
                    patched[1] = queryPacket[dnsStart + 1]
                    return patched
                }
                return answer
            }
            dnsCache.remove(domain)
        }

        return try {
            // fresh socket per query: immune to dropped/reordered replies,
            // and fully parallel across the pool
            DatagramSocket().let { socket ->
                try {
                    socket.soTimeout = 2500
                    protect(socket) // bypass VPN, go straight to network
                    socket.send(
                        DatagramPacket(
                            queryPacket,
                            dnsStart,
                            dnsLength,
                            InetAddress.getByName(UPSTREAM_DNS),
                            53,
                        ),
                    )
                    val answerBuf = ByteArray(4096)
                    val answerPacket = DatagramPacket(answerBuf, answerBuf.size)
                    socket.receive(answerPacket)
                    val answer = answerBuf.copyOf(answerPacket.length)
                    if (answer.size in 12..2048) {
                        dnsCache[domain] = Pair(answer, now + 300_000L)
                        sweepCache(now)
                    }
                    answer
                } finally {
                    socket.close()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upstream resolve failed: ${e.message}")
            null
        }
    }

    /** Occasionally evict expired entries so the map never grows unbounded. P2-5: LRU cap 500. */
    private fun sweepCache(now: Long) {
        // Also cap immediately if over 500
        if (dnsCache.size > 500) {
            val sorted = dnsCache.entries.sortedBy { it.value.second }
            val toRemove = dnsCache.size - 400
            for (i in 0 until toRemove) dnsCache.remove(sorted[i].key)
        }
        if (now - cacheSweptAt < 60_000L) return
        cacheSweptAt = now
        dnsCache.entries.removeIf { it.value.second < now }
    }

    /**
     * A calm, rotating reminder whenever a blocked site is intercepted.
     * Throttled to one Toast per 10 seconds so ad-heavy pages don't spam.
     * P1-1: now category-aware and adaptive via ChallengeRepository.
     */
    private fun showBlockToast(category: String = "porn", attempt: Int = 1) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastToastAt < 10_000L) return
        lastToastAt = now

        val intervention = ChallengeRepository.select(this, category, attempt)
        val message = intervention.message

        android.os.Handler(mainLooper).post {
            android.widget.Toast.makeText(
                this, message, android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    /**
     * The اتقي الله screen over the browser when a blocked site is queried.
     * Requires the "display over other apps" grant (the Android 14+ exemption
     * that lets a service launch an activity); otherwise falls back to the
     * reminder Toast only. Throttled to one screen per minute.
     * P1-1: carries category + attempt for adaptive content.
     */
    private fun maybeShowBlockScreen(domain: String, category: String = "porn", attempt: Int = 1) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockScreenAt < 60_000L) return
        if (!android.provider.Settings.canDrawOverlays(this)) return
        lastBlockScreenAt = now
        android.os.Handler(mainLooper).post {
            val intent = Intent(this, AppLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("blocked_site", domain)
                putExtra("blocked_category", category)
                putExtra("blocked_attempt", attempt)
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
                // never let the reminder break the filter
            }
        }
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
