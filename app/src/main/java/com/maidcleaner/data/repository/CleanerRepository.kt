package com.maidcleaner.data.repository

import com.maidcleaner.data.local.dao.ScanHistoryDao
import com.maidcleaner.data.local.entity.ScanHistoryEntity
import com.maidcleaner.data.model.JunkFile
import com.maidcleaner.data.model.ScanSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanerRepository @Inject constructor(
    private val scanHistoryDao: ScanHistoryDao
) {
    /**
     * Delete a list of files. Returns the number of bytes freed.
     * All deletions are permanent (after user confirmation).
     */
    suspend fun deleteFiles(files: List<JunkFile>): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        for (file in files) {
            val f = File(file.path)
            if (f.exists()) {
                val size = if (f.isDirectory) {
                    f.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                } else {
                    f.length()
                }
                val deleted = if (f.isDirectory) {
                    f.deleteRecursively()
                } else {
                    f.delete()
                }
                if (deleted) freedBytes += size
            }
        }
        freedBytes
    }

    /**
     * Delete files by path. Returns bytes freed.
     */
    suspend fun deletePaths(paths: List<String>): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        for (path in paths) {
            val f = File(path)
            if (f.exists()) {
                val size = if (f.isDirectory) {
                    f.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                } else {
                    f.length()
                }
                val deleted = if (f.isDirectory) f.deleteRecursively() else f.delete()
                if (deleted) freedBytes += size
            }
        }
        freedBytes
    }

    /**
     * Record a scan result in history.
     */
    suspend fun recordScan(
        scanType: String,
        junkSize: Long,
        fileCount: Int,
        wasCleaned: Boolean
    ) {
        scanHistoryDao.insert(
            ScanHistoryEntity(
                scanType = scanType,
                junkSize = junkSize,
                fileCount = fileCount,
                timestamp = System.currentTimeMillis(),
                wasCleaned = wasCleaned
            )
        )
    }

    fun getScanHistory() = scanHistoryDao.getAll()
}
