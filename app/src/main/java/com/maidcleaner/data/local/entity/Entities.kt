package com.maidcleaner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.maidcleaner.data.model.ListType
import com.maidcleaner.data.model.ScanFrequency
import com.maidcleaner.data.model.ScanModule

/**
 * Whitelist/Blacklist entry entity.
 */
@Entity(
    tableName = "whitelist",
    indices = [Index(value = ["path"], unique = true)]
)
data class WhitelistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val packageName: String,
    val type: ListType
)

/**
 * Scheduled scan entity.
 */
@Entity(tableName = "scheduled_scans")
data class ScheduledScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isEnabled: Boolean = true,
    val frequency: ScanFrequency,
    val hourOfDay: Int,
    val minuteOfHour: Int,
    val modulesCsv: String, // comma-separated ScanModule names
    val lastRunTime: Long = 0L,
    val nextRunTime: Long = 0L
) {
    val modules: Set<ScanModule>
        get() = modulesCsv.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { ScanModule.valueOf(it) }.getOrNull() }
            .toSet()

    companion object {
        fun fromModules(modules: Set<ScanModule>): String =
            modules.joinToString(",") { it.name }
    }
}

/**
 * Scan result history entity for tracking past scans.
 */
@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanType: String, // module name
    val junkSize: Long,
    val fileCount: Int,
    val timestamp: Long,
    val wasCleaned: Boolean
)
