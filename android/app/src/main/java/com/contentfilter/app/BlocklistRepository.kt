package com.contentfilter.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages the blocklist: loads the bundled asset on first run,
 * then syncs with the NestJS backend.
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
            val domains = context.assets.open("initial_blocklist.txt")
                .bufferedReader().readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { BlockedDomain(it) }
            dao.insertAll(domains)
        }
    }

    suspend fun syncWithBackend(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val localVersion = prefs.getString("version", "0.0.0")!!
            val remoteVersion = api().getVersion()
            if (remoteVersion == localVersion) {
                return@withContext SyncResult.UpToDate
            }
            val domains: List<BlockedDomain> = try {
                api().getDiff(localVersion).map { BlockedDomain(it) }
            } catch (e: Exception) {
                api().getBlocklist().map { BlockedDomain(it) }
            }
            if (domains.isNotEmpty()) {
                dao.insertAll(domains)
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
