package com.maidcleaner.data.local.dao

import androidx.room.*
import com.maidcleaner.data.local.entity.ScheduledScanEntity
import com.maidcleaner.data.local.entity.ScanHistoryEntity
import com.maidcleaner.data.local.entity.WhitelistEntity
import com.maidcleaner.data.model.ListType
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {
    @Query("SELECT * FROM whitelist WHERE type = :type")
    fun getByType(type: ListType): Flow<List<WhitelistEntity>>

    @Query("SELECT * FROM whitelist WHERE packageName = :packageName")
    fun getByPackageName(packageName: String): Flow<List<WhitelistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM whitelist WHERE path = :path AND type = :type)")
    suspend fun exists(path: String, type: ListType): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WhitelistEntity)

    @Delete
    suspend fun delete(entry: WhitelistEntity)

    @Query("DELETE FROM whitelist WHERE path = :path")
    suspend fun deleteByPath(path: String)
}

@Dao
interface ScheduledScanDao {
    @Query("SELECT * FROM scheduled_scans")
    fun getAll(): Flow<List<ScheduledScanEntity>>

    @Query("SELECT * FROM scheduled_scans WHERE isEnabled = 1")
    fun getEnabled(): Flow<List<ScheduledScanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ScheduledScanEntity): Long

    @Update
    suspend fun update(schedule: ScheduledScanEntity)

    @Delete
    suspend fun delete(schedule: ScheduledScanEntity)
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE scanType = :type ORDER BY timestamp DESC LIMIT 10")
    fun getByType(type: String): Flow<List<ScanHistoryEntity>>

    @Insert
    suspend fun insert(history: ScanHistoryEntity)

    @Query("DELETE FROM scan_history WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}
