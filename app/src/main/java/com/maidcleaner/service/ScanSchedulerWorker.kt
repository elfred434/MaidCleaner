package com.maidcleaner.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.maidcleaner.data.model.ScanFrequency
import com.maidcleaner.data.model.ScanModule
import com.maidcleaner.data.model.ScanProgress
import com.maidcleaner.data.model.ScheduledScan
import com.maidcleaner.data.repository.CleanerRepository
import com.maidcleaner.data.repository.SchedulerRepository
import com.maidcleaner.data.scanner.CorpseFinderScanner
import com.maidcleaner.data.scanner.SystemCleanerScanner
import com.maidcleaner.util.SizeFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class ScanSchedulerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val schedulerRepository: SchedulerRepository,
    private val corpseFinderScanner: CorpseFinderScanner,
    private val systemCleanerScanner: SystemCleanerScanner,
    private val cleanerRepository: CleanerRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, -1)
        if (scheduleId == -1L) return Result.failure()

        // Get the schedule
        val schedules = schedulerRepository.getEnabledSchedules().first()
        val schedule = schedules.find { it.id == scheduleId } ?: return Result.failure()

        var totalJunkSize = 0L
        var totalFileCount = 0

        val storageRoot = getStorageRoot()

        // Run each selected module
        for (module in schedule.modules) {
            try {
                when (module) {
                    ScanModule.CORPSE_FINDER -> {
                        val result = corpseFinderScanner.scan(storageRoot).first {
                            it is ScanProgress.Complete
                        }
                        if (result is ScanProgress.Complete) {
                            totalJunkSize += result.result.sumOf { it.totalSize }
                            totalFileCount += result.result.sumOf { it.fileCount }
                        }
                    }
                    ScanModule.SYSTEM_CLEANER -> {
                        val result = systemCleanerScanner.scan(storageRoot).first {
                            it is ScanProgress.Complete
                        }
                        if (result is ScanProgress.Complete) {
                            totalJunkSize += result.result.sumOf { it.totalSize }
                            totalFileCount += result.result.sumOf { it.files.size }
                        }
                    }
                    ScanModule.APP_CLEANER, ScanModule.DUPLICATE_FINDER -> {
                        // These are more intensive; skip in automated scans
                    }
                }
            } catch (_: Exception) {
                // Continue with next module on failure
            }
        }

        // Record result
        cleanerRepository.recordScan(
            scanType = "scheduled",
            junkSize = totalJunkSize,
            fileCount = totalFileCount,
            wasCleaned = false
        )

        // Show notification
        showScanResultNotification(totalJunkSize, totalFileCount)

        return Result.success()
    }

    private fun showScanResultNotification(junkSize: Long, fileCount: Int) {
        val channelId = CHANNEL_ID
        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("MaidCleaner Scan Complete")
            .setContentText(
                "Found $fileCount junk files (${SizeFormatter.format(junkSize)} reclaimable)"
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission not granted; skip notification
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scan Results",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for scheduled scan results"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun getStorageRoot(): String =
        System.getenv("EXTERNAL_STORAGE") ?: "/sdcard"

    companion object {
        const val KEY_SCHEDULE_ID = "schedule_id"
        const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "scan_results"

        fun scheduleWork(
            workManager: WorkManager,
            schedule: ScheduledScan
        ) {
            val inputData = workDataOf(KEY_SCHEDULE_ID to schedule.id)

            val repeatInterval = when (schedule.frequency) {
                ScanFrequency.DAILY -> 1L
                ScanFrequency.WEEKLY -> 7L
                ScanFrequency.CUSTOM -> 3L
            }
            val timeUnit = TimeUnit.DAYS

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ScanSchedulerWorker>(
                repeatInterval, timeUnit
            )
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                "scheduled_scan_${schedule.id}",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancelWork(workManager: WorkManager, scheduleId: Long) {
            workManager.cancelUniqueWork("scheduled_scan_$scheduleId")
        }
    }
}
