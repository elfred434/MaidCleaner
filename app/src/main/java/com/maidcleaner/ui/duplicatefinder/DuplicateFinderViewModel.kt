package com.maidcleaner.ui.duplicatefinder

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidcleaner.data.model.DuplicateFile
import com.maidcleaner.data.model.DuplicateGroup
import com.maidcleaner.data.model.FileType
import com.maidcleaner.data.model.ScanProgress
import com.maidcleaner.data.repository.CleanerRepository
import com.maidcleaner.data.scanner.DuplicateFinderScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuplicateFinderState(
    val isScanning: Boolean = false,
    val scanProgress: Int = 0,
    val scanMessage: String = "",
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val selectedForDeletion: Set<String> = emptySet(),
    val keptFiles: Set<String> = emptySet(),
    val totalSpaceReclaimable: Long = 0L,
    val expandedGroups: Set<Int> = emptySet(),
    val showConfirmDialog: Boolean = false,
    val fileTypes: Set<FileType> = FileType.entries.toSet()
)

@HiltViewModel
class DuplicateFinderViewModel @Inject constructor(
    private val duplicateFinderScanner: DuplicateFinderScanner,
    private val cleanerRepository: CleanerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DuplicateFinderState())
    val state: StateFlow<DuplicateFinderState> = _state.asStateFlow()

    fun scan() {
        if (_state.value.isScanning) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isScanning = true,
                duplicateGroups = emptyList(),
                selectedForDeletion = emptySet(),
                keptFiles = emptySet()
            )

            val storageRoot = Environment.getExternalStorageDirectory().absolutePath

            duplicateFinderScanner.scan(storageRoot, _state.value.fileTypes).collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning -> {
                        _state.value = _state.value.copy(
                            scanProgress = progress.percent,
                            scanMessage = progress.message
                        )
                    }
                    is ScanProgress.Complete -> {
                        val totalWasted = progress.result.sumOf { it.spaceWasted }
                        _state.value = _state.value.copy(
                            isScanning = false,
                            duplicateGroups = progress.result,
                            totalSpaceReclaimable = totalWasted
                        )
                    }
                    is ScanProgress.Error -> {
                        _state.value = _state.value.copy(isScanning = false)
                    }
                }
            }
        }
    }

    fun toggleGroupExpanded(index: Int) {
        val current = _state.value.expandedGroups
        _state.value = _state.value.copy(
            expandedGroups = if (index in current) current - index else current + index
        )
    }

    fun markAsKept(path: String) {
        _state.value = _state.value.copy(
            keptFiles = _state.value.keptFiles + path,
            selectedForDeletion = _state.value.selectedForDeletion - path
        )
        recalculateReclaimable()
    }

    fun toggleForDeletion(path: String) {
        if (path in _state.value.keptFiles) return
        val current = _state.value.selectedForDeletion
        _state.value = _state.value.copy(
            selectedForDeletion = if (path in current) current - path else current + path
        )
        recalculateReclaimable()
    }

    private fun recalculateReclaimable() {
        val selectedSize = _state.value.duplicateGroups
            .flatMap { it.files }
            .filter { it.path in _state.value.selectedForDeletion }
            .sumOf { it.size }
        _state.value = _state.value.copy(totalSpaceReclaimable = selectedSize)
    }

    fun showConfirmDialog() {
        _state.value = _state.value.copy(showConfirmDialog = true)
    }

    fun hideConfirmDialog() {
        _state.value = _state.value.copy(showConfirmDialog = false)
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val paths = _state.value.selectedForDeletion.toList()
            val freed = cleanerRepository.deletePaths(paths)

            cleanerRepository.recordScan(
                scanType = "duplicate_finder",
                junkSize = freed,
                fileCount = paths.size,
                wasCleaned = true
            )

            // Remove deleted files from groups
            val updatedGroups = _state.value.duplicateGroups
                .map { group ->
                    group.copy(
                        files = group.files.filter { it.path !in _state.value.selectedForDeletion }
                    )
                }
                .filter { it.files.size >= 2 } // Remove groups that no longer have duplicates

            _state.value = _state.value.copy(
                showConfirmDialog = false,
                duplicateGroups = updatedGroups,
                selectedForDeletion = emptySet(),
                totalSpaceReclaimable = 0L
            )
        }
    }
}
