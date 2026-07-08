package com.maidcleaner.ui.appcleaner

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidcleaner.data.model.AppInfo
import com.maidcleaner.data.model.ScanProgress
import com.maidcleaner.data.scanner.AppScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppCleanerState(
    val isScanning: Boolean = false,
    val scanProgress: Int = 0,
    val apps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val showExcessiveOnly: Boolean = false,
    val totalCacheSize: Long = 0L
)

@HiltViewModel
class AppCleanerViewModel @Inject constructor(
    private val appScanner: AppScanner
) : ViewModel() {

    private val _state = MutableStateFlow(AppCleanerState())
    val state: StateFlow<AppCleanerState> = _state.asStateFlow()

    init {
        scanApps()
    }

    fun scanApps() {
        if (_state.value.isScanning) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isScanning = true)

            appScanner.scanApps().collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning -> {
                        _state.value = _state.value.copy(scanProgress = progress.percent)
                    }
                    is ScanProgress.Complete -> {
                        val apps = progress.result
                        _state.value = _state.value.copy(
                            isScanning = false,
                            apps = apps,
                            filteredApps = apps,
                            totalCacheSize = apps.sumOf { it.cacheSize }
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

    fun toggleShowExcessiveOnly() {
        _state.value = _state.value.copy(
            showExcessiveOnly = !_state.value.showExcessiveOnly
        )
        applyFilters()
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.apps

        if (current.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.appName.contains(current.searchQuery, ignoreCase = true) ||
                    it.packageName.contains(current.searchQuery, ignoreCase = true)
            }
        }

        if (current.showExcessiveOnly) {
            filtered = filtered.filter { it.hasExcessiveCache }
        }

        _state.value = _state.value.copy(filteredApps = filtered)
    }

    /**
     * Open the system app info settings page for cache clearing.
     * Without root/Shizuku, we must rely on the system settings UI.
     */
    fun clearAppCache(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun clearAllCaches(context: Context) {
        // Without root, we can only direct users to the system settings
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }
}
