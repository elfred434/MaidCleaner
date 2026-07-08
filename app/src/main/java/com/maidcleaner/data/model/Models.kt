package com.maidcleaner.data.model

/**
 * Represents an orphaned file/folder left behind by an uninstalled app.
 */
data class CorpseFile(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val likelyPackageName: String,
    val childCount: Int = 0,
    val isSelected: Boolean = false,
    val isWhitelisted: Boolean = false,
    val lastModified: Long = 0L,
    val location: CorpseLocation = CorpseLocation.UNKNOWN
)

enum class CorpseLocation(val displayName: String, val path: String) {
    ANDROID_DATA("Android/Data", "Android/data"),
    ANDROID_OBB("Android/Obb", "Android/obb"),
    ANDROID_MEDIA("Android/Media", "Android/media"),
    APP_EXTERNAL("App External", ""),
    DOWNLOADS("Downloads", "Download"),
    TEMP("Temporary", ""),
    UNKNOWN("Unknown", "")
}

/**
 * Aggregated corpse group by package name.
 */
data class CorpseGroup(
    val packageName: String,
    val files: List<CorpseFile>,
    val totalSize: Long,
    val isWhitelisted: Boolean = false
) {
    val fileCount: Int get() = files.size
}

/**
 * Junk file categories for System Cleaner.
 */
data class JunkCategory(
    val type: JunkType,
    val files: List<JunkFile>,
    val totalSize: Long
)

enum class JunkType(val displayName: String) {
    CACHE("App Cache"),
    LOGS("Log Files"),
    TEMP("Temporary Files"),
    EMPTY_DIRS("Empty Folders"),
    APK("Obsolete APKs"),
    THUMBNAILS("Thumbnail Cache")
}

data class JunkFile(
    val path: String,
    val name: String,
    val size: Long,
    val type: JunkType,
    val isSelected: Boolean = false,
    val lastModified: Long = 0L
)

/**
 * App storage info for App Cleaner & App Control.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val appSize: Long,
    val cacheSize: Long,
    val dataSize: Long,
    val totalSize: Long,
    val installDate: Long,
    val lastUpdateTime: Long,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val versionName: String?,
    val versionCode: Long,
    val iconPath: String? = null,
    val hasExcessiveCache: Boolean = cacheSize > 100 * 1024 * 1024 // >100MB
)

/**
 * Storage category for Storage Analyzer.
 */
data class StorageCategory(
    val name: String,
    val size: Long,
    val percentage: Float,
    val color: Long
)

/**
 * File entry for Storage Analyzer drill-down.
 */
data class FileEntry(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
    val fileType: FileType,
    val children: List<FileEntry> = emptyList()
)

enum class FileType {
    VIDEO, AUDIO, IMAGE, DOCUMENT, ARCHIVE, APK, OTHER
}

/**
 * Duplicate file group.
 */
data class DuplicateGroup(
    val hash: String,
    val fileSize: Long,
    val files: List<DuplicateFile>,
    val spaceWasted: Long // (count - 1) * fileSize
)

data class DuplicateFile(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isSelected: Boolean = false,
    val isKept: Boolean = false,
    val thumbnailPath: String? = null
)

/**
 * Database info for Optimizer.
 */
data class DatabaseInfo(
    val path: String,
    val name: String,
    val packageName: String,
    val size: Long,
    val estimatedSavings: Long = 0,
    val isOptimizable: Boolean = false
)

/**
 * Scan result summary for dashboard and notifications.
 */
data class ScanSummary(
    val junkSize: Long = 0,
    val junkFileCount: Int = 0,
    val corpseSize: Long = 0,
    val corpseFileCount: Int = 0,
    val duplicateSize: Long = 0,
    val duplicateGroupCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Scheduled scan configuration.
 */
data class ScheduledScan(
    val id: Long = 0,
    val isEnabled: Boolean = true,
    val frequency: ScanFrequency = ScanFrequency.WEEKLY,
    val hourOfDay: Int = 2, // 2 AM
    val minuteOfHour: Int = 0,
    val modules: Set<ScanModule> = setOf(ScanModule.SYSTEM_CLEANER, ScanModule.CORPSE_FINDER),
    val lastRunTime: Long = 0L,
    val nextRunTime: Long = 0L
)

enum class ScanFrequency { DAILY, WEEKLY, CUSTOM }

enum class ScanModule {
    CORPSE_FINDER,
    SYSTEM_CLEANER,
    APP_CLEANER,
    DUPLICATE_FINDER
}

/**
 * Storage stats for dashboard.
 */
data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val usedPercentage: Float = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) * 100f else 0f
)

/**
 * Whitelist/Blacklist entry.
 */
data class WhitelistEntry(
    val id: Long = 0,
    val path: String,
    val packageName: String,
    val type: ListType
)

enum class ListType { WHITELIST, BLACKLIST }

/**
 * Root/Shizuku status.
 */
data class RootStatus(
    val isRooted: Boolean = false,
    val isShizukuAvailable: Boolean = false,
    val shizukuVersion: String? = null,
    val hasFullAccess: Boolean = false
)

/**
 * Generic scan progress wrapper used by all scanners.
 */
sealed class ScanProgress<T> {
    data class Scanning<T>(val percent: Int, val message: String) : ScanProgress<T>()
    data class Complete<T>(val result: T) : ScanProgress<T>()
    data class Error<T>(val message: String) : ScanProgress<T>()
}
