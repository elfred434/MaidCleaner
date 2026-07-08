package com.maidcleaner.data.repository

import com.maidcleaner.data.local.dao.ScheduledScanDao
import com.maidcleaner.data.local.entity.ScheduledScanEntity
import com.maidcleaner.data.model.ScanFrequency
import com.maidcleaner.data.model.ScanModule
import com.maidcleaner.data.model.ScheduledScan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchedulerRepository @Inject constructor(
    private val scheduledScanDao: ScheduledScanDao
) {
    fun getAllSchedules(): Flow<List<ScheduledScan>> =
        scheduledScanDao.getAll().map { entities ->
            entities.map { it.toModel() }
        }

    fun getEnabledSchedules(): Flow<List<ScheduledScan>> =
        scheduledScanDao.getEnabled().map { entities ->
            entities.map { it.toModel() }
        }

    suspend fun saveSchedule(schedule: ScheduledScan): Long {
        return scheduledScanDao.insert(schedule.toEntity())
    }

    suspend fun updateSchedule(schedule: ScheduledScan) {
        scheduledScanDao.update(schedule.toEntity())
    }

    suspend fun deleteSchedule(schedule: ScheduledScan) {
        scheduledScanDao.delete(schedule.toEntity())
    }

    private fun ScheduledScanEntity.toModel() = ScheduledScan(
        id = id,
        isEnabled = isEnabled,
        frequency = frequency,
        hourOfDay = hourOfDay,
        minuteOfHour = minuteOfHour,
        modules = modules,
        lastRunTime = lastRunTime,
        nextRunTime = nextRunTime
    )

    private fun ScheduledScan.toEntity() = ScheduledScanEntity(
        id = id,
        isEnabled = isEnabled,
        frequency = frequency,
        hourOfDay = hourOfDay,
        minuteOfHour = minuteOfHour,
        modulesCsv = ScheduledScanEntity.fromModules(modules),
        lastRunTime = lastRunTime,
        nextRunTime = nextRunTime
    )
}
