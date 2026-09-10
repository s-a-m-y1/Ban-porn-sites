package com.contentfilter.app

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction

@Entity(tableName = "blocked_domains")
data class BlockedDomain(
    @PrimaryKey val domain: String,
    val category: String = "porn",
)

@Entity(tableName = "custom_domains")
data class CustomDomain(
    @PrimaryKey val domain: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey val day: String, // yyyy-MM-dd
    val blockedCount: Int = 0,
    val topDomains: String = "", // "domain1:5,domain2:3,..."
)

@Dao
interface BlockedDomainDao {

    @Query("SELECT COUNT(*) FROM blocked_domains WHERE domain = :domain")
    suspend fun isBlocked(domain: String): Int

    @Query("SELECT COUNT(*) FROM blocked_domains WHERE domain = :domain AND category IN (:cats)")
    suspend fun isBlockedIn(domain: String, cats: List<String>): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(domains: List<BlockedDomain>)

    @Query("SELECT COUNT(*) FROM blocked_domains")
    suspend fun count(): Int

    @Query("DELETE FROM blocked_domains WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    // custom user-added domains
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustom(domain: CustomDomain)

    @Query("DELETE FROM custom_domains WHERE domain = :domain")
    suspend fun removeCustom(domain: String)

    @Query("SELECT * FROM custom_domains ORDER BY createdAt DESC")
    suspend fun listCustom(): List<CustomDomain>

    @Query("SELECT COUNT(*) FROM custom_domains WHERE domain = :domain")
    suspend fun isCustomBlocked(domain: String): Int

    // daily stats
    @Query("SELECT * FROM daily_stats WHERE day = :day")
    suspend fun getStat(day: String): DailyStat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStat(stat: DailyStat)

    @Query("SELECT * FROM daily_stats ORDER BY day DESC LIMIT :days")
    suspend fun recentStats(days: Int): List<DailyStat>

    @Query("SELECT * FROM daily_stats ORDER BY day")
    suspend fun allStats(): List<DailyStat>
}

@Database(
    entities = [BlockedDomain::class, CustomDomain::class, DailyStat::class],
    version = 2,
    exportSchema = false,
)
abstract class BlocklistDatabase : RoomDatabase() {
    abstract fun blockedDomainDao(): BlockedDomainDao

    companion object {
        @Volatile private var instance: BlocklistDatabase? = null

        fun getInstance(context: android.content.Context): BlocklistDatabase =
            instance ?: synchronized(this) {
                instance ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    BlocklistDatabase::class.java,
                    "blocklist.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
