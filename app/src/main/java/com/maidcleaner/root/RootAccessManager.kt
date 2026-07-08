package com.maidcleaner.root

import android.content.Context
import com.maidcleaner.data.model.RootStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootAccessManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _rootStatus = MutableStateFlow(detectRootStatus())
    val rootStatus: StateFlow<RootStatus> = _rootStatus.asStateFlow()

    /**
     * Check if the device is rooted by testing common root indicators.
     */
    private fun detectRootStatus(): RootStatus {
        val isRooted = checkRootAccess()
        val isShizukuAvailable = checkShizukuAvailable()
        val shizukuVersion = getShizukuVersion()

        return RootStatus(
            isRooted = isRooted,
            isShizukuAvailable = isShizukuAvailable,
            shizukuVersion = shizukuVersion,
            hasFullAccess = isRooted || isShizukuAvailable
        )
    }

    fun refresh() {
        _rootStatus.value = detectRootStatus()
    }

    private fun checkRootAccess(): Boolean {
        // Method 1: Check for su binary
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in suPaths) {
            if (java.io.File(path).exists()) return true
        }

        // Method 2: Try executing su
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun checkShizukuAvailable(): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getShizukuVersion(): String? {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            info.versionName
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Execute a command with root privileges.
     * Returns the command output or null on failure.
     */
    fun executeAsRoot(command: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode == 0) output.trim() else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Execute a Shizuku command (requires Shizuku to be running and permission granted).
     */
    fun executeViaShizuku(command: String): String? {
        // Shizuku integration would use the Shizuku API to execute commands
        // For now, this is a placeholder that returns null
        // Full implementation would require Shizuku binder initialization
        return try {
            // rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            null
        } catch (_: Exception) {
            null
        }
    }
}
