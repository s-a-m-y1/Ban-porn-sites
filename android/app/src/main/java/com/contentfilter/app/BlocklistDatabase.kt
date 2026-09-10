package com.contentfilter.app

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "blocked_domains")
data class BlockedDomain(
    @PrimaryKey val domain: String,
    val category: String = "adult-content",
)

@Dao
interface BlockedDomainDao {
    @Query("SELECT COUNT(*) FROM blocked_domains WHERE domain = :domain")
    suspend fun isBlocked(domain: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(domains: List<BlockedDomain>)

    @Query("DELETE FROM blocked_domains WHERE domain NOT IN (:domains)")
    suspend fun removeMissing(domains: List<String>): Int

    @Query("SELECT COUNT(*) FROM blocked_domains")
    suspend fun count(): Int

    @Query("DELETE FROM blocked_domains")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(domains: List<BlockedDomain>) {
        clear()
        insertAll(domains)
    }
}

@Database(entities = [BlockedDomain::class], version = 1, exportSchema = false)
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
                ).build().also { instance = it }
            }
    }
}
