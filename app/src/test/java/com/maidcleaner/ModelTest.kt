package com.maidcleaner

import com.maidcleaner.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ModelTest {

    @Test
    fun corpseGroupTotalSize() {
        val group = CorpseGroup(
            packageName = "com.example.app",
            files = listOf(
                CorpseFile("/path/1", "dir1", 1000L, true, "com.example.app"),
                CorpseFile("/path/2", "dir2", 2000L, true, "com.example.app")
            ),
            totalSize = 3000L
        )
        assertEquals(3000L, group.totalSize)
        assertEquals(2, group.fileCount)
    }

    @Test
    fun storageStatsPercentage() {
        val stats = StorageStats(
            totalBytes = 1000L,
            usedBytes = 750L,
            freeBytes = 250L
        )
        assertEquals(75f, stats.usedPercentage, 0.1f)
    }

    @Test
    fun duplicateGroupSpaceWasted() {
        val group = DuplicateGroup(
            hash = "abc123",
            fileSize = 1000L,
            files = listOf(
                DuplicateFile("/path/1", "file1.txt", 1000L, 0L),
                DuplicateFile("/path/2", "file2.txt", 1000L, 0L),
                DuplicateFile("/path/3", "file3.txt", 1000L, 0L)
            ),
            spaceWasted = 2000L // 2 copies * 1000 bytes
        )
        assertEquals(2000L, group.spaceWasted)
        assertEquals(3, group.files.size)
    }

    @Test
    fun appInfoExcessiveCache() {
        val app = AppInfo(
            packageName = "com.example.bigapp",
            appName = "Big App",
            appSize = 50000000L,
            cacheSize = 200000000L, // 200MB - exceeds 100MB threshold
            dataSize = 30000000L,
            totalSize = 280000000L,
            installDate = 0L,
            lastUpdateTime = 0L,
            isSystemApp = false,
            isEnabled = true,
            versionName = "1.0",
            versionCode = 1
        )
        assertTrue(app.hasExcessiveCache)
    }

    @Test
    fun appInfoNormalCache() {
        val app = AppInfo(
            packageName = "com.example.smallapp",
            appName = "Small App",
            appSize = 5000000L,
            cacheSize = 50000000L, // 50MB - under 100MB threshold
            dataSize = 3000000L,
            totalSize = 58000000L,
            installDate = 0L,
            lastUpdateTime = 0L,
            isSystemApp = false,
            isEnabled = true,
            versionName = "1.0",
            versionCode = 1
        )
        assertFalse(app.hasExcessiveCache)
    }

    @Test
    fun scheduledScanEntityModules() {
        val entity = com.maidcleaner.data.local.entity.ScheduledScanEntity(
            id = 1,
            isEnabled = true,
            frequency = ScanFrequency.WEEKLY,
            hourOfDay = 2,
            minuteOfHour = 0,
            modulesCsv = "SYSTEM_CLEANER,CORPSE_FINDER"
        )
        assertEquals(setOf(ScanModule.SYSTEM_CLEANER, ScanModule.CORPSE_FINDER), entity.modules)
    }

    @Test
    fun scheduledScanEntityFromModules() {
        val csv = com.maidcleaner.data.local.entity.ScheduledScanEntity.fromModules(
            setOf(ScanModule.APP_CLEANER, ScanModule.DUPLICATE_FINDER)
        )
        assertTrue(csv.contains("APP_CLEANER"))
        assertTrue(csv.contains("DUPLICATE_FINDER"))
    }
}
