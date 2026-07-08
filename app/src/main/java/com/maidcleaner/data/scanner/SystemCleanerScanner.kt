package com.maidcleaner.data.scanner

import com.maidcleaner.data.model.JunkCategory
import com.maidcleaner.data.model.JunkFile
import com.maidcleaner.data.model.JunkType
import com.maidcleaner.data.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemCleanerScanner @Inject constructor() {

    /**
     * Paths and patterns to search for junk files.
     * Respects Scoped Storage: only accesses directories the app has
     * permission to read.
     */
    private val cachePatterns = listOf(
        "cache", "Cache", ".cache", "caches"
    )
    private val tempPatterns = listOf(
        "tmp", "temp", ".tmp", ".temp", "Temp"
    )
    private val logPatterns = listOf(
        ".log", "log", "logs", ".log.txt"
    )
    private val thumbnailPatterns = listOf(
        "thumbnails", ".thumbnails", "thumb", "Thumb"
    )

    fun scan(storageRoot: String): Flow<ScanProgress<List<JunkCategory>>> = flow {
        emit(ScanProgress.Scanning(0, "Scanning for junk files..."))

        val categories = mutableMapOf<JunkType, MutableList<JunkFile>>()
        JunkType.entries.forEach { categories[it] = mutableListOf() }

        // Scan for cache files
        emit(ScanProgress.Scanning(15, "Scanning cache directories..."))
        scanForCacheFiles(storageRoot, categories)

        // Scan for temp files
        emit(ScanProgress.Scanning(30, "Scanning temporary files..."))
        scanForTempFiles(storageRoot, categories)

        // Scan for log files
        emit(ScanProgress.Scanning(50, "Scanning log files..."))
        scanForLogFiles(storageRoot, categories)

        // Scan for empty directories
        emit(ScanProgress.Scanning(65, "Scanning empty directories..."))
        scanForEmptyDirs(storageRoot, categories)

        // Scan for thumbnail caches
        emit(ScanProgress.Scanning(80, "Scanning thumbnail caches..."))
        scanForThumbnails(storageRoot, categories)

        // Scan for obsolete APKs
        emit(ScanProgress.Scanning(90, "Scanning APK files..."))
        scanForObsoleteApks(storageRoot, categories)

        val result = categories.map { (type, files) ->
            JunkCategory(
                type = type,
                files = files.sortedByDescending { it.size },
                totalSize = files.sumOf { it.size }
            )
        }.filter { it.files.isNotEmpty() }

        emit(ScanProgress.Complete(result))
    }.flowOn(Dispatchers.IO)

    private fun scanForCacheFiles(
        root: String,
        categories: MutableMap<JunkType, MutableList<JunkFile>>
    ) {
        val androidDataDir = File(root, "Android/data")
        if (androidDataDir.exists()) {
            androidDataDir.listFiles()?.forEach { appDir ->
                appDir.listFiles()?.forEach { subdir ->
                    if (cachePatterns.any { subdir.name.equals(it, ignoreCase = true) }) {
                        addDirectoryContents(subdir, JunkType.CACHE, categories)
                    }
                }
            }
        }
    }

    private fun scanForTempFiles(
        root: String,
        categories: MutableMap<JunkType, MutableList<JunkFile>>
    ) {
        scanDirectoryForPatterns(File(root), tempPatterns, JunkType.TEMP, categories, maxDepth = 4)
    }

    private fun scanForLogFiles(
        root: String,
        categories: MutableMap<JunkType, MutableList<JunkFile>>
    ) {
        scanDirectoryForPatterns(File(root), logPatterns, JunkType.LOGS, categories, maxDepth = 4)
    }

    private fun scanForEmptyDirs(
        root: String,
        categories: MutableMap<JunkType, MutableList<JunkFile>>
    ) {
        findEmptyDirectories(File(root), categories, maxDepth = 5)
    }

    private fun scanForThumbnails(
        root: String,
        categories: MutableMap<JunkType, MutableList<JunkFile>>
    ) {
        val dcimDir = File(root, "DCIM")
        val picturesDir = File(root, "Pictures")
        listOf(dcimDir, picturesDir).forEach { dir ->
            if (dir.exists()) {
                dir.listFiles()?.forEach { subdir ->
                    if (thumbnailPatterns.any { subdir.name.equals(it, ignoreCase = true) }) {
                        addDirectoryContents(subdir, JunkType.THUMBNAILS, categories)
                    }
                }
            }
        }
    }

    private fun scanForObsoleteApks(
        root: String,
        categories: MutableMap<JunkType, MutableList<JunkFile>>
    ) {
        val downloadDir = File(root, "Download")
        if (downloadDir.exists()) {
            downloadDir.walkTopDown()
                .maxDepth(2)
                .filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }
                .forEach { apk ->
                    categories[JunkType.APK]?.add(
                        JunkFile(
                            path = apk.absolutePath,
                            name = apk.name,
                            size = apk.length(),
                            type = JunkType.APK,
                            lastModified = apk.lastModified()
                        )
                    )
                }
        }
    }

    private fun addDirectoryContents(
        dir: File,
        type: JunkType,
        categories: MutableMap<JunkType, MutableList<JunkFile>>
    ) {
        if (!dir.exists() || !dir.isDirectory) return
        dir.walkTopDown().maxDepth(3).forEach { file ->
            if (file.isFile) {
                categories[type]?.add(
                    JunkFile(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        type = type,
                        lastModified = file.lastModified()
                    )
                )
            }
        }
    }

    private fun scanDirectoryForPatterns(
        dir: File,
        patterns: List<String>,
        type: JunkType,
        categories: MutableMap<JunkType, MutableList<JunkFile>>,
        maxDepth: Int
    ) {
        if (!dir.exists() || maxDepth <= 0) return
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (patterns.any { file.name.equals(it, ignoreCase = true) }) {
                    addDirectoryContents(file, type, categories)
                } else {
                    scanDirectoryForPatterns(file, patterns, type, categories, maxDepth - 1)
                }
            } else {
                if (patterns.any { file.name.endsWith(it, ignoreCase = true) }) {
                    categories[type]?.add(
                        JunkFile(
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            type = type,
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }
    }

    private fun findEmptyDirectories(
        dir: File,
        categories: MutableMap<JunkType, MutableList<JunkFile>>,
        maxDepth: Int
    ) {
        if (!dir.exists() || !dir.isDirectory || maxDepth <= 0) return
        dir.listFiles()?.let { children ->
            if (children.isEmpty()) {
                categories[JunkType.EMPTY_DIRS]?.add(
                    JunkFile(
                        path = dir.absolutePath,
                        name = dir.name,
                        size = 0L,
                        type = JunkType.EMPTY_DIRS,
                        lastModified = dir.lastModified()
                    )
                )
            } else {
                children.filter { it.isDirectory }.forEach { subdir ->
                    findEmptyDirectories(subdir, categories, maxDepth - 1)
                }
            }
        }
    }
}
