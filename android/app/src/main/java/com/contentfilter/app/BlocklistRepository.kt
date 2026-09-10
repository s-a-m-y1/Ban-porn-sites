package com.contentfilter.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages the blocklist: loads category assets, handles custom user domains,
 * and syncs with the NestJS backend.
 */
class BlocklistRepository(private val context: Context) {

    private val dao: BlockedDomainDao =
        BlocklistDatabase.getInstance(context).blockedDomainDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("blocklist_prefs", Context.MODE_PRIVATE)

    private fun api(): ApiService =
        ApiService.create(prefs.getString("backend_url", "http://10.0.2.2:3000")!!)

    suspend fun ensureInitialBlocklist() = withContext(Dispatchers.IO) {
        if (dao.count() == 0) {
            Categories.ALL.forEach { cat -> loadCategoryAsset(cat) }
        }
    }

    private suspend fun loadCategoryAsset(category: String) {
        val assetFile = when (category) {
            Categories.PORN -> "blocklist_porn.txt"
            Categories.GAMBLING -> "blocklist_gambling.txt"
            Categories.FAKENEWS -> "blocklist_fakenews.txt"
            Categories.MALWARE -> "blocklist_malware.txt"
            else -> return
        }
        val domains = context.assets.open(assetFile)
            .bufferedReader().readLines()
            .mapNotNull { line ->
                val l = line.trim()
                when {
                    l.isEmpty() || l.startsWith("#") -> null
                    // hosts format: "0.0.0.0 domain.com"
                    l.startsWith("0.0.0.0 ") -> l.substringAfter("0.0.0.0 ").trim()
                        .takeIf { it.contains(".") }
                    // plain domain list
                    else -> l.takeIf { !it.contains(" ") && it.contains(".") }
                }
            }
            .map { BlockedDomain(it, category) }
        // chunked inserts to keep transactions small
        domains.chunked(5000).forEach { dao.insertAll(it) }
    }

    /** Enable or disable a category (load from asset / delete rows). */
    suspend fun setCategoryEnabled(category: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        if (enabled) {
            loadCategoryAsset(category)
        } else {
            dao.deleteByCategory(category)
        }
    }

    // ---- custom domains ----
    suspend fun addCustom(domain: String): Boolean = withContext(Dispatchers.IO) {
        val d = domain.trim().lowercase()
            .removePrefix("http://").removePrefix("https://")
            .removePrefix("www.")
            .trim().trimEnd('/')
        if (d.isEmpty() || !d.contains(".") || d.contains(" ")) return@withContext false
        dao.insertCustom(CustomDomain(d))
        true
    }

    suspend fun removeCustom(domain: String) = withContext(Dispatchers.IO) {
        dao.removeCustom(domain)
    }

    suspend fun listCustom(): List<CustomDomain> = dao.listCustom()

    // ---- stats ----
    suspend fun dailyStats(days: Int = 7): List<DailyStat> = dao.recentStats(days)

    suspend fun allStats(): List<Pair<String, Int>> =
        dao.allStats().map { it.day to it.blockedCount }

    // ---- backend sync ----
    suspend fun syncWithBackend(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val localVersion = prefs.getString("version", "0.0.0")!!
            val remoteVersion = api().getVersion()
            if (remoteVersion == localVersion) {
                return@withContext SyncResult.UpToDate
            }
            val domains: List<BlockedDomain> = try {
                api().getDiff(localVersion).map { BlockedDomain(it, Categories.PORN) }
            } catch (e: Exception) {
                api().getBlocklist().map { BlockedDomain(it, Categories.PORN) }
            }
            if (domains.isNotEmpty()) {
                domains.chunked(5000).forEach { dao.insertAll(it) }
            }
            prefs.edit().putString("version", remoteVersion).apply()
            SyncResult.Updated(domains.size)
        } catch (e: Exception) {
            SyncResult.Failed(e.message ?: "network error")
        }
    }

    suspend fun count(): Int = dao.count()

    sealed class SyncResult {
        object UpToDate : SyncResult()
        data class Updated(val added: Int) : SyncResult()
        data class Failed(val error: String) : SyncResult()
    }
}
