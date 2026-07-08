package com.maidcleaner.ui.optimizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidcleaner.data.model.DatabaseInfo
import com.maidcleaner.data.model.RootStatus
import com.maidcleaner.data.model.ScanProgress
import com.maidcleaner.data.scanner.DatabaseOptimizerScanner
import com.maidcleaner.root.RootAccessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OptimizerState(
    val isScanning: Boolean = false,
    val databases: List<DatabaseInfo> = emptyList(),
    val rootStatus: RootStatus = RootStatus(),
    val isOptimizing: Boolean = false,
    val optimizingDbName: String? = null,
    val optimizationResults: Map<String, Pair<Long, Long>> = emptyMap(),
    val totalSavings: Long = 0L
)

@HiltViewModel
class OptimizerViewModel @Inject constructor(
    private val databaseOptimizerScanner: DatabaseOptimizerScanner,
    private val rootAccessManager: RootAccessManager
) : ViewModel() {

    private val _state = MutableStateFlow(OptimizerState())
    val state: StateFlow<OptimizerState> = _state.asStateFlow()

    init {
        observeRootStatus()
    }

    private fun observeRootStatus() {
        viewModelScope.launch {
            rootAccessManager.rootStatus.collect { status ->
                _state.value = _state.value.copy(rootStatus = status)
            }
        }
    }

    fun scan() {
        if (_state.value.isScanning) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isScanning = true)

            val hasRoot = _state.value.rootStatus.hasFullAccess

            databaseOptimizerScanner.scan(hasRoot).collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning -> { /* progress */ }
                    is ScanProgress.Complete -> {
                        _state.value = _state.value.copy(
                            isScanning = false,
                            databases = progress.result
                        )
                    }
                    is ScanProgress.Error -> {
                        _state.value = _state.value.copy(isScanning = false)
                    }
                }
            }
        }
    }

    fun optimizeDatabase(dbInfo: DatabaseInfo) {
        if (_state.value.isOptimizing) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isOptimizing = true,
                optimizingDbName = dbInfo.name
            )

            val hasRoot = _state.value.rootStatus.hasFullAccess
            databaseOptimizerScanner.vacuumDatabase(dbInfo.path, hasRoot).collect { progress ->
                when (progress) {
                    is ScanProgress.Scanning -> { /* optimizing */ }
                    is ScanProgress.Complete -> {
                        val (sizeBefore, sizeAfter) = progress.result
                        val savings = sizeBefore - sizeAfter
                        _state.value = _state.value.copy(
                            isOptimizing = false,
                            optimizingDbName = null,
                            optimizationResults = _state.value.optimizationResults +
                                (dbInfo.name to Pair(sizeBefore, sizeAfter)),
                            totalSavings = _state.value.totalSavings + savings,
                            databases = _state.value.databases.map { db ->
                                if (db.path == dbInfo.path) {
                                    db.copy(
                                        size = sizeAfter,
                                        estimatedSavings = 0,
                                        isOptimizable = false
                                    )
                                } else db
                            }
                        )
                    }
                    is ScanProgress.Error -> {
                        _state.value = _state.value.copy(
                            isOptimizing = false,
                            optimizingDbName = null
                        )
                    }
                }
            }
        }
    }

    fun optimizeAll() {
        val optimizable = _state.value.databases.filter { it.isOptimizable }
        for (db in optimizable) {
            optimizeDatabase(db)
        }
    }
}
