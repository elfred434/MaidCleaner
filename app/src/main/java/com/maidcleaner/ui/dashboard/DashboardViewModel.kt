package com.maidcleaner.ui.dashboard

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidcleaner.data.model.RootStatus
import com.maidcleaner.data.model.ScanProgress
import com.maidcleaner.data.model.ScanSummary
import com.maidcleaner.data.model.StorageStats
import com.maidcleaner.data.scanner.CorpseFinderScanner
import com.maidcleaner.data.scanner.SystemCleanerScanner
import com.maidcleaner.root.RootAccessManager
import com.maidcleaner.util.StorageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val storageStats: StorageStats = StorageStats(0, 0, 0),
    val scanSummary: ScanSummary = ScanSummary(),
    val isQuickScanning: Boolean = false,
    val quickScanProgress: Int = 0,
    val quickScanMessage: String = "",
    val rootStatus: RootStatus = RootStatus()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val corpseFinderScanner: CorpseFinderScanner,
    private val systemCleanerScanner: SystemCleanerScanner,
    private val rootAccessManager: RootAccessManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadStorageStats()
        observeRootStatus()
    }

    private fun loadStorageStats() {
        val root = Environment.getExternalStorageDirectory().absolutePath
        _state.value = _state.value.copy(
            storageStats = StorageUtils.getStorageStats(root)
        )
    }

    private fun observeRootStatus() {
        viewModelScope.launch {
            rootAccessManager.rootStatus.collect { status ->
                _state.value = _state.value.copy(rootStatus = status)
            }
        }
    }

    fun quickScan() {
        if (_state.value.isQuickScanning) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isQuickScanning = true)

            val storageRoot = Environment.getExternalStorageDirectory().absolutePath
            var junkSize = 0L
            var junkFileCount = 0
            var corpseSize = 0L
            var corpseFileCount = 0

            // Scan junk files
            systemCleanerScanner.scan(storageRoot).collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning -> {
                        _state.value = _state.value.copy(
                            quickScanProgress = progress.percent / 2,
                            quickScanMessage = progress.message
                        )
                    }
                    is ScanProgress.Complete -> {
                        junkSize = progress.result.sumOf { it.totalSize }
                        junkFileCount = progress.result.sumOf { it.files.size }
                    }
                    is ScanProgress.Error -> { /* handle error */ }
                }
            }

            // Scan corpse files
            corpseFinderScanner.scan(storageRoot).collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning -> {
                        _state.value = _state.value.copy(
                            quickScanProgress = 50 + progress.percent / 2,
                            quickScanMessage = progress.message
                        )
                    }
                    is ScanProgress.Complete -> {
                        corpseSize = progress.result.sumOf { it.totalSize }
                        corpseFileCount = progress.result.sumOf { it.fileCount }
                    }
                    is ScanProgress.Error -> { /* handle error */ }
                }
            }

            _state.value = _state.value.copy(
                isQuickScanning = false,
                quickScanProgress = 100,
                scanSummary = ScanSummary(
                    junkSize = junkSize,
                    junkFileCount = junkFileCount,
                    corpseSize = corpseSize,
                    corpseFileCount = corpseFileCount
                )
            )

            // Refresh storage stats after scan
            loadStorageStats()
        }
    }

    fun refreshStorageStats() {
        loadStorageStats()
    }
}
