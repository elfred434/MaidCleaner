package com.maidcleaner.data.scanner

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.maidcleaner.data.model.AppInfo
import com.maidcleaner.data.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScanner @Inject constructor(
    private val packageManager: PackageManager
) {
    fun scanApps(): Flow<ScanProgress<List<AppInfo>>> = flow {
        emit(ScanProgress.Scanning(0, "Loading installed apps..."))

        val installedApps = packageManager.getInstalledApplications(0)
        val total = installedApps.size
        val apps = mutableListOf<AppInfo>()

        installedApps.forEachIndexed { index, appInfo ->
            val progress = ((index + 1) * 100) / total
            if (index % 10 == 0) {
                emit(ScanProgress.Scanning(progress, "Processing ${index + 1}/$total..."))
            }

            try {
                val pkgInfo = packageManager.getPackageInfo(
                    appInfo.packageName,
                    PackageManager.GET_META_DATA
                )

                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appSize = getAppSize(appInfo)
                val cacheSize = getCacheSize(appInfo)
                val dataSize = getDataSize(appInfo)

                apps.add(
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = appInfo.loadLabel(packageManager).toString(),
                        appSize = appSize,
                        cacheSize = cacheSize,
                        dataSize = dataSize,
                        totalSize = appSize + cacheSize + dataSize,
                        installDate = pkgInfo.firstInstallTime,
                        lastUpdateTime = pkgInfo.lastUpdateTime,
                        isSystemApp = isSystemApp,
                        isEnabled = appInfo.enabled,
                        versionName = pkgInfo.versionName,
                        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            pkgInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            pkgInfo.versionCode.toLong()
                        }
                    )
                )
            } catch (_: Exception) {
                // Skip apps we can't get info for
            }
        }

        emit(ScanProgress.Complete(apps.sortedByDescending { it.totalSize }))
    }.flowOn(Dispatchers.IO)

    private fun getAppSize(appInfo: ApplicationInfo): Long {
        val sourceDir = appInfo.sourceDir
        return try {
            val apkFile = File(sourceDir)
            val libSize = File(appInfo.nativeLibraryDir).walkTopDown()
                .filter { it.isFile }.sumOf { it.length() }
            apkFile.length() + libSize
        } catch (_: Exception) { 0L }
    }

    private fun getCacheSize(appInfo: ApplicationInfo): Long {
        // Without root/Shizuku, we can only estimate cache size from accessible paths
        val cacheDir = File(appInfo.dataDir, "cache")
        return if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }

    private fun getDataSize(appInfo: ApplicationInfo): Long {
        val dataDir = File(appInfo.dataDir)
        return if (dataDir.exists()) {
            dataDir.walkTopDown()
                .maxDepth(2)
                .filter { it.isFile }
                .sumOf { it.length() }
        } else 0L
    }
}
