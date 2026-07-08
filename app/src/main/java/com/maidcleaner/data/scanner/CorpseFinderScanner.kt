package com.maidcleaner.data.scanner

import android.content.pm.PackageManager
import com.maidcleaner.data.local.dao.WhitelistDao
import com.maidcleaner.data.model.CorpseFile
import com.maidcleaner.data.model.CorpseGroup
import com.maidcleaner.data.model.CorpseLocation
import com.maidcleaner.data.model.ListType
import com.maidcleaner.data.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorpseFinderScanner @Inject constructor(
    private val packageManager: PackageManager,
    private val whitelistDao: WhitelistDao
) {
    /**
     * Known app-data locations to scan for orphaned files.
     * On Scoped Storage (API 30+), we can only access app-specific directories
     * for our own app without MANAGE_EXTERNAL_STORAGE. We attempt what we can
     * and note limitations.
     */
    private val scanLocations = mapOf(
        CorpseLocation.ANDROID_DATA to "Android/data",
        CorpseLocation.ANDROID_OBB to "Android/obb",
        CorpseLocation.ANDROID_MEDIA to "Android/media"
    )

    /**
     * Get the set of currently installed package names for cross-referencing.
     */
    fun getInstalledPackages(): Set<String> {
        return packageManager.getInstalledApplications(0)
            .map { it.packageName }
            .toSet()
    }

    /**
     * Scan for orphaned files/directories.
     * Emits progress updates and then the final result.
     */
    fun scan(
        storageRoot: String,
        installedPackages: Set<String> = getInstalledPackages()
    ): Flow<ScanProgress<List<CorpseGroup>>> = flow {
        emit(ScanProgress.Scanning(0, "Starting scan..."))

        val orphans = mutableListOf<CorpseFile>()
        val totalLocations = scanLocations.size
        var currentLocation = 0

        for ((location, relativePath) in scanLocations) {
            currentLocation++
            val progress = (currentLocation * 100) / (totalLocations + 1)
            emit(ScanProgress.Scanning(progress, "Scanning $relativePath..."))

            val dir = File(storageRoot, relativePath)
            if (!dir.exists() || !dir.isDirectory) continue

            // List subdirectories in this location
            val subDirs = dir.listFiles()?.filter { it.isDirectory } ?: continue

            for (subDir in subDirs) {
                val dirName = subDir.name

                // Check if this directory belongs to an installed app
                if (!isInstalledPackage(dirName, installedPackages)) {
                    val size = calculateSize(subDir)
                    val childCount = countChildren(subDir)

                    // Check whitelist (suspend call — safe inside flow builder)
                    val isWhitelisted = withContext(Dispatchers.IO) {
                        whitelistDao.exists(subDir.absolutePath, ListType.WHITELIST)
                    }

                    if (!isWhitelisted) {
                        orphans.add(
                            CorpseFile(
                                path = subDir.absolutePath,
                                name = dirName,
                                size = size,
                                isDirectory = true,
                                likelyPackageName = dirName,
                                childCount = childCount,
                                lastModified = subDir.lastModified(),
                                location = location
                            )
                        )
                    }
                }
            }
        }

        // Also scan for orphaned config files in top-level directories
        emit(ScanProgress.Scanning(90, "Checking for config files..."))
        scanForOrphanedConfigFiles(storageRoot, installedPackages, orphans)

        // Group by package name
        val groups = orphans
            .groupBy { it.likelyPackageName }
            .map { (pkg, files) ->
                CorpseGroup(
                    packageName = pkg,
                    files = files,
                    totalSize = files.sumOf { it.size }
                )
            }
            .sortedByDescending { it.totalSize }

        emit(ScanProgress.Complete(groups))
    }.flowOn(Dispatchers.IO)

    private fun isInstalledPackage(dirName: String, installedPackages: Set<String>): Boolean {
        // Direct match
        if (dirName in installedPackages) return true

        // Some folders use only part of the package name or have suffixes
        // Check if any installed package starts with this directory name
        return installedPackages.any { pkg ->
            pkg == dirName || pkg.startsWith("$dirName.") || dirName.startsWith("$pkg.")
        }
    }

    private fun scanForOrphanedConfigFiles(
        storageRoot: String,
        installedPackages: Set<String>,
        orphans: MutableList<CorpseFile>
    ) {
        val root = File(storageRoot)

        root.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val dirName = dir.name
            // Check for dot-config directories that don't match installed packages
            if (dirName.startsWith(".")) {
                val baseName = dirName.removePrefix(".")
                val matchingPkg = installedPackages.any { pkg ->
                    pkg.contains(baseName, ignoreCase = true) ||
                        baseName.contains(pkg.split(".").lastOrNull() ?: "", ignoreCase = true)
                }
                if (!matchingPkg) {
                    val size = calculateSize(dir)
                    if (size > 0) {
                        orphans.add(
                            CorpseFile(
                                path = dir.absolutePath,
                                name = dirName,
                                size = size,
                                isDirectory = true,
                                likelyPackageName = baseName,
                                childCount = countChildren(dir),
                                lastModified = dir.lastModified(),
                                location = CorpseLocation.APP_EXTERNAL
                            )
                        )
                    }
                }
            }
        }
    }

    private fun calculateSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private fun countChildren(dir: File): Int {
        if (!dir.isDirectory) return 0
        return dir.walkTopDown().filter { it.isFile }.count()
    }
}
