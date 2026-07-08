package com.maidcleaner.ui.scheduler

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.maidcleaner.data.model.ScanFrequency
import com.maidcleaner.data.model.ScanModule
import com.maidcleaner.data.model.ScheduledScan
import com.maidcleaner.data.repository.SchedulerRepository
import com.maidcleaner.service.ScanSchedulerWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SchedulerState(
    val schedules: List<ScheduledScan> = emptyList(),
    val isCreating: Boolean = false,
    val newScheduleFrequency: ScanFrequency = ScanFrequency.WEEKLY,
    val newScheduleHour: Int = 2,
    val newScheduleMinute: Int = 0,
    val newScheduleModules: Set<ScanModule> = setOf(ScanModule.SYSTEM_CLEANER, ScanModule.CORPSE_FINDER),
    val hasNotificationPermission: Boolean = false,
    val exactAlarmPermissionNeeded: Boolean = false
)

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val schedulerRepository: SchedulerRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _state = MutableStateFlow(SchedulerState())
    val state: StateFlow<SchedulerState> = _state.asStateFlow()

    init {
        loadSchedules()
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            schedulerRepository.getAllSchedules().collect { schedules ->
                _state.value = _state.value.copy(schedules = schedules)
            }
        }
    }

    fun setFrequency(frequency: ScanFrequency) {
        _state.value = _state.value.copy(newScheduleFrequency = frequency)
    }

    fun setHour(hour: Int) {
        _state.value = _state.value.copy(newScheduleHour = hour)
    }

    fun setMinute(minute: Int) {
        _state.value = _state.value.copy(newScheduleMinute = minute)
    }

    fun toggleModule(module: ScanModule) {
        val current = _state.value.newScheduleModules
        _state.value = _state.value.copy(
            newScheduleModules = if (module in current) current - module else current + module
        )
    }

    fun createSchedule() {
        viewModelScope.launch {
            val schedule = ScheduledScan(
                isEnabled = true,
                frequency = _state.value.newScheduleFrequency,
                hourOfDay = _state.value.newScheduleHour,
                minuteOfHour = _state.value.newScheduleMinute,
                modules = _state.value.newScheduleModules,
                nextRunTime = calculateNextRunTime(
                    _state.value.newScheduleHour,
                    _state.value.newScheduleMinute
                )
            )

            val id = schedulerRepository.saveSchedule(schedule)
            val savedSchedule = schedule.copy(id = id)

            // Schedule the WorkManager job
            ScanSchedulerWorker.scheduleWork(workManager, savedSchedule)

            _state.value = _state.value.copy(isCreating = false)
        }
    }

    fun toggleScheduleEnabled(schedule: ScheduledScan) {
        viewModelScope.launch {
            val updated = schedule.copy(isEnabled = !schedule.isEnabled)
            schedulerRepository.updateSchedule(updated)

            if (updated.isEnabled) {
                ScanSchedulerWorker.scheduleWork(workManager, updated)
            } else {
                ScanSchedulerWorker.cancelWork(workManager, updated.id)
            }
        }
    }

    fun deleteSchedule(schedule: ScheduledScan) {
        viewModelScope.launch {
            schedulerRepository.deleteSchedule(schedule)
            ScanSchedulerWorker.cancelWork(workManager, schedule.id)
        }
    }

    fun showCreateSheet() {
        _state.value = _state.value.copy(isCreating = true)
    }

    fun hideCreateSheet() {
        _state.value = _state.value.copy(isCreating = false)
    }

    private fun calculateNextRunTime(hour: Int, minute: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }
}
