package com.maidcleaner.ui.systemcleaner

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidcleaner.data.model.JunkCategory
import com.maidcleaner.data.model.JunkFile
import com.maidcleaner.data.model.JunkType
import com.maidcleaner.data.model.ScanProgress
import com.maidcleaner.data.repository.CleanerRepository
import com.maidcleaner.data.scanner.SystemCleanerScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SystemCleanerState(
    val isScanning: Boolean = false,
    val scanProgress: Int = 0,
    val scanMessage: String = "",
    val junkCategories: List<JunkCategory> = emptyList(),
    val expandedCategories: Set<JunkType> = emptySet(),
    val selectedFiles: Set<String> = emptySet(),
    val totalReclaimable: Long = 0L,
    val showConfirmDialog: Boolean = false,
    val sizeBefore: Long = 0L,
    val sizeAfter: Long = 0L,
    val isCleaning: Boolean = false,
    val cleaningComplete: Boolean = false
)

@HiltViewModel
class SystemCleanerViewModel @Inject constructor(
    private val systemCleanerScanner: SystemCleanerScanner,
    private val cleanerRepository: CleanerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SystemCleanerState())
    val state: StateFlow<SystemCleanerState> = _state.asStateFlow()

    fun scan() {
        if (_state.value.isScanning) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isScanning = true,
                scanProgress = 0,
                junkCategories = emptyList(),
                cleaningComplete = false,
                selectedFiles = emptySet()
            )

            val storageRoot = Environment.getExternalStorageDirectory().absolutePath

            systemCleanerScanner.scan(storageRoot).collect { progress ->
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
                            junkCategories = progress.result,
                            totalReclaimable = totalSize,
                            sizeBefore = totalSize
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

    fun toggleCategoryExpanded(type: JunkType) {
        val current = _state.value.expandedCategories
        _state.value = _state.value.copy(
            expandedCategories = if (type in current) current - type else current + type
        )
    }

    fun toggleFileSelection(path: String) {
        val current = _state.value.selectedFiles
        val newSelection = if (path in current) current - path else current + path
        val selectedSize = _state.value.junkCategories
            .flatMap { it.files }
            .filter { it.path in newSelection }
            .sumOf { it.size }
        _state.value = _state.value.copy(
            selectedFiles = newSelection,
            totalReclaimable = selectedSize
        )
    }

    fun selectAllInCategory(type: JunkType) {
        val category = _state.value.junkCategories.find { it.type == type } ?: return
        val newSelection = _state.value.selectedFiles + category.files.map { it.path }.toSet()
        val selectedSize = _state.value.junkCategories
            .flatMap { it.files }
            .filter { it.path in newSelection }
            .sumOf { it.size }
        _state.value = _state.value.copy(
            selectedFiles = newSelection,
            totalReclaimable = selectedSize
        )
    }

    fun selectAll() {
        val allPaths = _state.value.junkCategories.flatMap { it.files.map { f -> f.path } }.toSet()
        _state.value = _state.value.copy(
            selectedFiles = allPaths,
            totalReclaimable = _state.value.junkCategories.sumOf { it.totalSize }
        )
    }

    fun showConfirmDialog() {
        _state.value = _state.value.copy(showConfirmDialog = true)
    }

    fun hideConfirmDialog() {
        _state.value = _state.value.copy(showConfirmDialog = false)
    }

    fun cleanSelected() {
        val selectedFiles = _state.value.junkCategories
            .flatMap { it.files }
            .filter { it.path in _state.value.selectedFiles }

        viewModelScope.launch {
            _state.value = _state.value.copy(isCleaning = true, showConfirmDialog = false)

            val freed = cleanerRepository.deleteFiles(selectedFiles)

            cleanerRepository.recordScan(
                scanType = "system_cleaner",
                junkSize = freed,
                fileCount = selectedFiles.size,
                wasCleaned = true
            )

            // Recalculate remaining junk
            val remainingCategories = _state.value.junkCategories.map { category ->
                category.copy(
                    files = category.files.filter { it.path !in _state.value.selectedFiles },
                )
            }.map { category ->
                category.copy(totalSize = category.files.sumOf { it.size })
            }.filter { it.files.isNotEmpty() }

            _state.value = _state.value.copy(
                isCleaning = false,
                cleaningComplete = true,
                sizeAfter = _state.value.sizeBefore - freed,
                junkCategories = remainingCategories,
                selectedFiles = emptySet(),
                totalReclaimable = remainingCategories.sumOf { it.totalSize }
            )
        }
    }
}
