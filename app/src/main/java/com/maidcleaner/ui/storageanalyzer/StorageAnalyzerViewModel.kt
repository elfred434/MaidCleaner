package com.maidcleaner.ui.storageanalyzer

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidcleaner.data.model.FileEntry
import com.maidcleaner.data.model.FileType
import com.maidcleaner.data.model.ScanProgress
import com.maidcleaner.data.model.StorageCategory
import com.maidcleaner.data.scanner.StorageAnalyzerScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageAnalyzerState(
    val isScanning: Boolean = false,
    val scanProgress: Int = 0,
    val scanMessage: String = "",
    val categories: List<StorageCategory> = emptyList(),
    val largestFiles: List<FileEntry> = emptyList(),
    val filteredFiles: List<FileEntry> = emptyList(),
    val selectedTypeFilter: FileType? = null,
    val drillDownStack: List<FileEntry> = emptyList()
)

@HiltViewModel
class StorageAnalyzerViewModel @Inject constructor(
    private val storageAnalyzerScanner: StorageAnalyzerScanner
) : ViewModel() {

    private val _state = MutableStateFlow(StorageAnalyzerState())
    val state: StateFlow<StorageAnalyzerState> = _state.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        if (_state.value.isScanning) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isScanning = true)

            val storageRoot = Environment.getExternalStorageDirectory().absolutePath

            storageAnalyzerScanner.scan(storageRoot).collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning -> {
                        _state.value = _state.value.copy(
                            scanProgress = progress.percent,
                            scanMessage = progress.message
                        )
                    }
                    is ScanProgress.Complete -> {
                        val (categories, files) = progress.result
                        _state.value = _state.value.copy(
                            isScanning = false,
                            categories = categories,
                            largestFiles = files,
                            filteredFiles = files
                        )
                    }
                    is ScanProgress.Error -> {
                        _state.value = _state.value.copy(isScanning = false)
                    }
                }
            }
        }
    }

    fun setTypeFilter(type: FileType?) {
        _state.value = _state.value.copy(
            selectedTypeFilter = type,
            filteredFiles = if (type == null) {
                _state.value.largestFiles
            } else {
                _state.value.largestFiles.filter { it.fileType == type }
            }
        )
    }

    fun drillDownInto(entry: FileEntry) {
        if (!entry.isDirectory) return
        val children = storageAnalyzerScanner.drillDown(java.io.File(entry.path))
        _state.value = _state.value.copy(
            drillDownStack = _state.value.drillDownStack + entry,
            filteredFiles = children
        )
    }

    fun navigateUp(): Boolean {
        val stack = _state.value.drillDownStack
        if (stack.isEmpty()) return false
        _state.value = _state.value.copy(
            drillDownStack = stack.dropLast(1)
        )
        val newStack = _state.value.drillDownStack
        if (newStack.isEmpty()) {
            _state.value = _state.value.copy(
                filteredFiles = if (_state.value.selectedTypeFilter == null)
                    _state.value.largestFiles
                else
                    _state.value.largestFiles.filter { it.fileType == _state.value.selectedTypeFilter }
            )
        } else {
            val parent = newStack.last()
            val children = storageAnalyzerScanner.drillDown(java.io.File(parent.path))
            _state.value = _state.value.copy(filteredFiles = children)
        }
        return true
    }
}
