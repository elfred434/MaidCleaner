package com.maidcleaner.ui.corpsefinder

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidcleaner.data.model.CorpseGroup
import com.maidcleaner.data.model.ScanProgress
import com.maidcleaner.data.repository.CleanerRepository
import com.maidcleaner.data.repository.WhitelistRepository
import com.maidcleaner.data.scanner.CorpseFinderScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CorpseFinderState(
    val isScanning: Boolean = false,
    val scanProgress: Int = 0,
    val scanMessage: String = "",
    val corpseGroups: List<CorpseGroup> = emptyList(),
    val selectedGroups: Set<String> = emptySet(),
    val totalReclaimable: Long = 0L,
    val showConfirmDialog: Boolean = false,
    val showWhitelistDialog: Boolean = false,
    val lastScanTimestamp: Long = 0L
)

@HiltViewModel
class CorpseFinderViewModel @Inject constructor(
    private val corpseFinderScanner: CorpseFinderScanner,
    private val cleanerRepository: CleanerRepository,
    private val whitelistRepository: WhitelistRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CorpseFinderState())
    val state: StateFlow<CorpseFinderState> = _state.asStateFlow()

    fun scan() {
        if (_state.value.isScanning) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isScanning = true,
                scanProgress = 0,
                corpseGroups = emptyList(),
                selectedGroups = emptySet()
            )

            val storageRoot = Environment.getExternalStorageDirectory().absolutePath

            corpseFinderScanner.scan(storageRoot).collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning -> {
                        _state.value = _state.value.copy(
                            scanProgress = progress.percent,
                            scanMessage = progress.message
                        )
                    }
                    is ScanProgress.Complete -> {
                        val totalSize = progress.result.sumOf { it.totalSize }
                        _state.value = _state.value.copy(
                            isScanning = false,
                            corpseGroups = progress.result,
                            totalReclaimable = totalSize,
                            lastScanTimestamp = System.currentTimeMillis()
                        )
                    }
                    is ScanProgress.Error -> {
                        _state.value = _state.value.copy(
                            isScanning = false,
                            scanMessage = progress.message
                        )
                    }
                }
            }
        }
    }

    fun toggleGroupSelection(packageName: String) {
        val current = _state.value.selectedGroups
        val newSelection = if (packageName in current) {
            current - packageName
        } else {
            current + packageName
        }
        _state.value = _state.value.copy(
            selectedGroups = newSelection,
            totalReclaimable = _state.value.corpseGroups
                .filter { it.packageName in newSelection }
                .sumOf { it.totalSize }
        )
    }

    fun selectAll() {
        val allPackages = _state.value.corpseGroups.map { it.packageName }.toSet()
        _state.value = _state.value.copy(
            selectedGroups = allPackages,
            totalReclaimable = _state.value.corpseGroups.sumOf { it.totalSize }
        )
    }

    fun deselectAll() {
        _state.value = _state.value.copy(
            selectedGroups = emptySet(),
            totalReclaimable = 0L
        )
    }

    fun showConfirmDialog() {
        _state.value = _state.value.copy(showConfirmDialog = true)
    }

    fun hideConfirmDialog() {
        _state.value = _state.value.copy(showConfirmDialog = false)
    }

    fun deleteSelected() {
        val selectedGroups = _state.value.corpseGroups
            .filter { it.packageName in _state.value.selectedGroups }

        viewModelScope.launch {
            val paths = selectedGroups.flatMap { group ->
                group.files.map { it.path }
            }
            val freed = cleanerRepository.deletePaths(paths)

            cleanerRepository.recordScan(
                scanType = "corpse_finder",
                junkSize = freed,
                fileCount = paths.size,
                wasCleaned = true
            )

            // Refresh the list
            _state.value = _state.value.copy(
                showConfirmDialog = false,
                corpseGroups = _state.value.corpseGroups.filter { it.packageName !in _state.value.selectedGroups },
                selectedGroups = emptySet(),
                totalReclaimable = 0L
            )
        }
    }

    fun addToWhitelist(packageName: String) {
        viewModelScope.launch {
            val group = _state.value.corpseGroups.find { it.packageName == packageName }
            group?.files?.forEach { file ->
                whitelistRepository.addToWhitelist(file.path, packageName)
            }
            // Remove from current results
            _state.value = _state.value.copy(
                corpseGroups = _state.value.corpseGroups.filter { it.packageName != packageName },
                selectedGroups = _state.value.selectedGroups - packageName
            )
        }
    }
}
