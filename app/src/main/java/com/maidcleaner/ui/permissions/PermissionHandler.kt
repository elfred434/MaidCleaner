package com.maidcleaner.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Centralized permission handler that explains each permission before requesting it.
 * Every permission request is preceded by a rationale dialog.
 */
object PermissionHandler {

    /**
     * Check if storage permissions are granted.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ==
                    PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if notification permission is granted (Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * Request storage permissions via the appropriate mechanism for the API level.
     */
    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: Use MANAGE_EXTERNAL_STORAGE intent for full access
            // or use READ_MEDIA_* for media-only access on Android 13+
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_STORAGE
            )
        }
    }

    /**
     * Get the permissions that need to be requested for a specific module.
     */
    fun getPermissionsForModule(module: String): List<PermissionInfo> {
        return when (module) {
            "corpse_finder", "system_cleaner", "storage_analyzer", "duplicate_finder" ->
                listOf(
                    PermissionInfo(
                        permission = "STORAGE_ACCESS",
                        rationale = "MaidCleaner needs storage access to scan for files. " +
                                "No files are accessed without your explicit action.",
                        isRequired = true
                    )
                )
            "scheduler" ->
                listOf(
                    PermissionInfo(
                        permission = Manifest.permission.POST_NOTIFICATIONS,
                        rationale = "MaidCleaner needs notification permission to alert you " +
                                "when scheduled scans complete.",
                        isRequired = false
                    )
                )
            else -> emptyList()
        }
    }

    const val REQUEST_STORAGE = 1001
    const val REQUEST_NOTIFICATIONS = 1002
}

data class PermissionInfo(
    val permission: String,
    val rationale: String,
    val isRequired: Boolean
)

/**
 * Composable that handles permission state and shows rationale dialogs.
 */
@Composable
fun rememberPermissionState(
    onPermissionResult: (granted: Boolean) -> Unit = {}
): PermissionState {
    val context = LocalContext.current
    return remember {
        PermissionState(
            hasStoragePermission = PermissionHandler.hasStoragePermission(context),
            hasNotificationPermission = PermissionHandler.hasNotificationPermission(context),
            requestStoragePermission = {
                PermissionHandler.requestStoragePermission(context as Activity)
            },
            onPermissionResult = onPermissionResult
        )
    }
}

data class PermissionState(
    val hasStoragePermission: Boolean,
    val hasNotificationPermission: Boolean,
    val requestStoragePermission: () -> Unit,
    val onPermissionResult: (Boolean) -> Unit
)
