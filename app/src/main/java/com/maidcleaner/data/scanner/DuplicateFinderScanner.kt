package com.maidcleaner.data.scanner

import com.maidcleaner.data.model.DuplicateFile
import com.maidcleaner.data.model.DuplicateGroup
import com.maidcleaner.data.model.FileType
import com.maidcleaner.data.model.ScanProgress
import com.maidcleaner.util.FileClassifier
import com.maidcleaner.util.HashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateFinderScanner @Inject constructor() {

    /**
     * Scan for duplicate files using content hashing.
     * Strategy:
     * 1. Group all files by size first (files of different sizes cannot be duplicates)
     * 2. For size groups with 2+ files, compute quick hash (first/last 4KB)
     * 3. For files with matching quick hashes, compute full MD5
     * 4. Group by final hash → these are true duplicates
     */
    fun scan(
        storageRoot: String,
        fileTypes: Set<FileType> = FileType.entries.toSet()
    ): Flow<ScanProgress<List<DuplicateGroup>>> = flow {
        emit(ScanProgress.Scanning(0, "Collecting files..."))

        // Phase 1: Collect files by size
        val filesBySize = mutableMapOf<Long, MutableList<File>>()
        val root = File(storageRoot)
        var fileCount = 0

        root.walkTopDown().maxDepth(8).forEach { file ->
            if (file.isFile && file.length() > 0) {
                val fileType = FileClassifier.classify(file.name)
                if (fileType in fileTypes) {
                    // Skip very small files (< 1KB) to reduce noise
                    if (file.length() >= 1024) {
                        filesBySize.getOrPut(file.length()) { mutableListOf() }.add(file)
                        fileCount++
                    }
                }
            }
        }

        emit(ScanProgress.Scanning(10, "Found $fileCount files, checking sizes..."))

        // Phase 2: Filter to size groups with 2+ files (potential duplicates)
        val potentialDuplicates = filesBySize.filter { it.value.size >= 2 }
        val totalGroups = potentialDuplicates.size
        var processedGroups = 0

        emit(ScanProgress.Scanning(20, "Quick-hashing ${potentialDuplicates.values.sumOf { it.size }} candidates..."))

        // Phase 3: Quick hash comparison
        val filesByQuickHash = mutableMapOf<String, MutableList<File>>()

        for ((_, files) in potentialDuplicates) {
            processedGroups++
            if (processedGroups % 100 == 0) {
                val progress = 20 + ((processedGroups * 50) / totalGroups.coerceAtLeast(1))
                emit(ScanProgress.Scanning(progress.coerceAtMost(70), "Quick-hashing..."))
            }

            for (file in files) {
                try {
                    val quickHash = HashUtils.quickHash(file)
                    val key = "${file.length()}:$quickHash"
                    filesByQuickHash.getOrPut(key) { mutableListOf() }.add(file)
                } catch (_: Exception) {
                    // Skip files we can't read
                }
            }
        }

        // Phase 4: Full hash for quick-hash matches
        val filesByFullHash = mutableMapOf<String, MutableList<File>>()
        val hashCandidates = filesByQuickHash.filter { it.value.size >= 2 }
        var hashProcessed = 0

        emit(ScanProgress.Scanning(70, "Full-hashing ${hashCandidates.values.sumOf { it.size }} candidates..."))

        for ((_, files) in hashCandidates) {
            hashProcessed++
            if (hashProcessed % 10 == 0) {
                val progress = 70 + ((hashProcessed * 25) / hashCandidates.size.coerceAtLeast(1))
                emit(ScanProgress.Scanning(progress.coerceAtMost(95), "Computing full hashes..."))
            }

            for (file in files) {
                try {
                    val fullHash = HashUtils.md5(file)
                    filesByFullHash.getOrPut(fullHash) { mutableListOf() }.add(file)
                } catch (_: Exception) {
                    // Skip unreadable files
                }
            }
        }

        // Phase 5: Build duplicate groups
        emit(ScanProgress.Scanning(95, "Building duplicate groups..."))

        val groups = filesByFullHash
            .filter { it.value.size >= 2 }
            .map { (hash, files) ->
                val size = files.first().length()
                DuplicateGroup(
                    hash = hash,
                    fileSize = size,
                    files = files.map { file ->
                        DuplicateFile(
                            path = file.absolutePath,
                            name = file.name,
                            size = size,
                            lastModified = file.lastModified()
                        )
                    },
                    spaceWasted = (files.size - 1) * size
                )
            }
            .sortedByDescending { it.spaceWasted }

        emit(ScanProgress.Complete(groups))
    }.flowOn(Dispatchers.IO)
}
