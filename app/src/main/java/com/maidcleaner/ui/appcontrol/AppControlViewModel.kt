package com.maidcleaner.ui.appcontrol

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidcleaner.data.model.AppInfo
import com.maidcleaner.data.model.ScanProgress
import com.maidcleaner.data.scanner.AppScanner
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class AppSortMode { NAME, SIZE, INSTALL_DATE }

data class AppControlState(
    val isScanning: Boolean = false,
    val apps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val sortMode: AppSortMode = AppSortMode.SIZE,
    val showSystemApps: Boolean = false,
    val selectedApps: Set<String> = emptySet(),
    val showExportSheet: Boolean = false,
    val exportedPath: String? = null
)

@HiltViewModel
class AppControlViewModel @Inject constructor(
    private val appScanner: AppScanner
) : ViewModel() {

    private val _state = MutableStateFlow(AppControlState())
    val state: StateFlow<AppControlState> = _state.asStateFlow()

    private val gson = Gson()

    init {
        scanApps()
    }

    fun scanApps() {
        if (_state.value.isScanning) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isScanning = true)

            appScanner.scanApps().collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning -> { /* loading */ }
                    is ScanProgress.Complete -> {
                        _state.value = _state.value.copy(
                            isScanning = false,
                            apps = progress.result
                        )
                        applyFilters()
                    }
                    is ScanProgress.Error -> {
                        _state.value = _state.value.copy(isScanning = false)
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters()
    }

    fun setSortMode(mode: AppSortMode) {
        _state.value = _state.value.copy(sortMode = mode)
        applyFilters()
    }

    fun toggleShowSystemApps() {
        _state.value = _state.value.copy(showSystemApps = !_state.value.showSystemApps)
        applyFilters()
    }

    fun toggleAppSelection(packageName: String) {
        val current = _state.value.selectedApps
        _state.value = _state.value.copy(
            selectedApps = if (packageName in current) current - packageName else current + packageName
        )
    }

    fun selectAll() {
        _state.value = _state.value.copy(
            selectedApps = _state.value.filteredApps.map { it.packageName }.toSet()
        )
    }

    fun deselectAll() {
        _state.value = _state.value.copy(selectedApps = emptySet())
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.apps

        if (!current.showSystemApps) {
            filtered = filtered.filter { !it.isSystemApp }
        }

        if (current.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.appName.contains(current.searchQuery, ignoreCase = true) ||
                    it.packageName.contains(current.searchQuery, ignoreCase = true)
            }
        }

        filtered = when (current.sortMode) {
            AppSortMode.NAME -> filtered.sortedBy { it.appName.lowercase() }
            AppSortMode.SIZE -> filtered.sortedByDescending { it.totalSize }
            AppSortMode.INSTALL_DATE -> filtered.sortedByDescending { it.installDate }
        }

        _state.value = _state.value.copy(filteredApps = filtered)
    }

    fun uninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun uninstallSelected(context: Context) {
        for (pkg in _state.value.selectedApps) {
            uninstallApp(context, pkg)
        }
    }

    fun forceStopApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun clearAppData(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun exportAppList(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val exportData = _state.value.filteredApps.map { app ->
                        mapOf(
                            "packageName" to app.packageName,
                            "appName" to app.appName,
                            "versionName" to (app.versionName ?: ""),
                            "versionCode" to app.versionCode,
                            "isSystemApp" to app.isSystemApp,
                            "installDate" to app.installDate,
                            "totalSize" to app.totalSize
                        )
                    }
                    val json = gson.toJson(exportData)
                    val exportDir = File(
                        context.getExternalFilesDir(null),
                        "exports"
                    )
                    exportDir.mkdirs()
                    val file = File(exportDir, "installed_apps_${System.currentTimeMillis()}.json")
                    file.writeText(json)
                    _state.value = _state.value.copy(exportedPath = file.absolutePath)
                } catch (_: Exception) { }
            }
        }
    }

    fun importAppList(context: Context, jsonContent: String): List<Map<String, Any>>? {
        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            gson.fromJson(jsonContent, type)
        } catch (_: Exception) { null }
    }
}
