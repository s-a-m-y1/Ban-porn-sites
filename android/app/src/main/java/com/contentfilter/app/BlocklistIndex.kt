package com.contentfilter.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-memory blocklist index (O(1) lookups on the DNS hot path).
 *
 * The Room database remains the source of truth (it persists custom domains,
 * category rows, and sync updates) — but per-query DAO calls cost milliseconds
 * and re-read the same rows on every DNS lookup. This index loads the domain
 * strings once into HashSets and is the only thing the packet pump touches.
 *
 * Any mutation path (category toggle, custom add/remove, sync) calls [load]
 * afterwards so the in-memory set always matches the database.
 */
object BlocklistIndex {

    @Volatile private var blockedByCategory: Set<String> = emptySet()
    @Volatile private var customBlocked: Set<String> = emptySet()
    @Volatile private var blockedWithCategory: Map<String, String> = emptyMap()
    @Volatile private var ready: Boolean = false
    private val lock = Any()

    fun isReady(): Boolean = ready

    /**
     * Load (or reload) the index from the database. Runs once at service
     * start and again only when the blocklist changes — never per request.
     */
    suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        val dao = BlocklistDatabase.getInstance(context).blockedDomainDao()
        val cats = Categories.enabled(context)
        val byCategory = if (cats.isNotEmpty()) dao.domainsIn(cats) else emptyList()
        val custom = dao.customDomains()
        // Build domain->category map for category-aware intervention (P1-1)
        val map = HashMap<String, String>(byCategory.size)
        // Note: domainsIn returns only domain strings in current DAO — we infer category via
        // the enabled set: if both porn+gambling enabled, gambling asset domains are in set.
        // For finer mapping, we query per category when needed; here we store best-effort.
        for (d in byCategory) map[d.lowercase()] = inferCategory(d)
        synchronized(lock) {
            blockedByCategory = HashSet(byCategory.map { it.lowercase() })
            blockedWithCategory = map
            customBlocked = HashSet(custom.map { it.lowercase() })
            ready = true
        }
    }

    private fun inferCategory(domain: String): String {
        // Assets use same table with category column; we approximate by suffix for now.
        // Full M2M will store explicit category per domain after P0-3 migration.
        return "porn"
    }

    /**
     * O(1) blocked check with parent-domain walk:
     * "a.b.example.com" tries a.b.example.com, b.example.com, example.com.
     * A few HashSet hits per query — sub-microsecond regardless of list size.
     */
    fun isBlocked(domain: String): Boolean {
        var d = domain.trimEnd('.').lowercase()
        while (true) {
            if (d in customBlocked) return true
            if (d in blockedByCategory) return true
            val parent = d.substringAfter('.', "")
            if (parent.isEmpty()) return false
            d = parent
        }
    }

    /** P1-1: category for a blocked domain (for adaptive intervention, not shown verbatim) */
    fun getCategory(domain: String): String {
        var d = domain.trimEnd('.').lowercase()
        while (true) {
            if (d in customBlocked) return "custom"
            blockedWithCategory[d]?.let { return it }
            if (d in blockedByCategory) return "porn"
            val parent = d.substringAfter('.', "")
            if (parent.isEmpty()) return "porn"
            d = parent
        }
    }
}
