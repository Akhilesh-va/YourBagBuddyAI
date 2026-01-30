package com.example.yourbagbuddy.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.yourbagbuddy.data.local.dao.ChecklistItemDao
import com.example.yourbagbuddy.data.local.dao.ReminderDao
import com.example.yourbagbuddy.data.local.dao.TripDao
import com.example.yourbagbuddy.domain.model.RepeatType
import com.example.yourbagbuddy.notification.PackingNotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker that runs at reminder time. Responsibilities:
 * 1. Load reminder and checklist/trip from Room
 * 2. Check if reminder is still enabled
 * 3. Check stop conditions (trip start reached, all items checked)
 * 4. If unchecked items exist, show notification; otherwise skip
 * 5. If repeat is DAILY/EVERY_X_DAYS, schedule next run; if ONCE or stop met, cancel
 */
@HiltWorker
class PackingReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val reminderDao: ReminderDao,
    private val tripDao: TripDao,
    private val checklistItemDao: ChecklistItemDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val checklistId = inputData.getString(KEY_CHECKLIST_ID) ?: run {
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.w(TAG, "Missing checklistId in work input")
            }
            return Result.failure()
        }
        if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
            android.util.Log.d(TAG, "Worker running for checklistId=$checklistId")
        }

        // Load reminder; if missing or disabled, stop (no spam)
        val reminder = reminderDao.getEnabledByChecklistId(checklistId) ?: run {
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "No enabled reminder for $checklistId, skipping")
            }
            return Result.success()
        }

        val now = System.currentTimeMillis()
        // Use the user-set "Trip starting time" (reminder.reminderTime), not the trip entity's
        // startDate (which is often the checklist creation date and would stop reminders too early).
        val reminderTimeMillis = reminder.reminderTime

        val uncheckedNames = checklistItemDao.getUncheckedItemNames(checklistId)
        if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
            android.util.Log.d(TAG, "Unchecked items for $checklistId: ${uncheckedNames.size} items")
        }

        // Show notification first when this run is at/around the reminder time and there are unchecked items.
        // Previously we checked "trip start reached" before notifying, so the one-time reminder at 11:28 PM
        // never fired when the worker ran at 11:28 or 11:29 (now >= reminderTime → exit without showing).
        if (uncheckedNames.isNotEmpty()) {
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "Showing notification for $checklistId with ${uncheckedNames.size} unchecked items")
            }
            PackingNotificationHelper.showPackingReminder(appContext, checklistId, uncheckedNames)
        }

        // Stop condition: user-set trip start date/time has passed — stop future reminders only after showing this one
        if (reminder.stopAtTripStart && now >= reminderTimeMillis) {
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "Trip start time reached for $checklistId, stopping reminder")
            }
            cancelReminderWork(appContext, checklistId)
            reminderDao.insert(reminder.copy(isEnabled = false))
            return Result.success()
        }

        // Stop condition: all items checked — no need for more reminders
        if (reminder.stopWhenCompleted && uncheckedNames.isEmpty()) {
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "All items checked for $checklistId, stopping reminder")
            }
            cancelReminderWork(appContext, checklistId)
            reminderDao.insert(reminder.copy(isEnabled = false))
            return Result.success()
        }

        // Reschedule for next occurrence if repeating
        val repeatType = RepeatType.valueOf(reminder.repeatType)
        when (repeatType) {
            RepeatType.ONCE -> {
                cancelReminderWork(appContext, checklistId)
                reminderDao.insert(reminder.copy(isEnabled = false))
            }
            RepeatType.DAILY -> scheduleNextRun(appContext, checklistId, reminder.reminderTime, 1)
            RepeatType.EVERY_X_DAYS -> {
                val interval = (reminder.repeatIntervalDays ?: 1).coerceAtLeast(1)
                scheduleNextRun(appContext, checklistId, reminder.reminderTime, interval)
            }
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "PackingReminderWorker"
        const val KEY_CHECKLIST_ID = "checklist_id"

        private const val REMINDER_UNIQUE_PREFIX = "packing_reminder_"
        private fun uniqueWorkName(checklistId: String) = "${REMINDER_UNIQUE_PREFIX}$checklistId"

        private fun cancelReminderWork(context: Context, checklistId: String) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(checklistId))
            } catch (e: Exception) {
                if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                    android.util.Log.w(TAG, "cancelReminderWork failed", e)
                }
            }
        }

        /**
         * Enqueue next OneTimeWorkRequest at the same clock time (reminderTime + intervalDays).
         * reminderTimeMillis is the user-set time; we run at that time each interval.
         */
        private fun scheduleNextRun(
            context: Context,
            checklistId: String,
            reminderTimeMillis: Long,
            intervalDays: Int
        ) {
            val input = Data.Builder().putString(KEY_CHECKLIST_ID, checklistId).build()
            val calendar = java.util.Calendar.getInstance().apply { timeInMillis = reminderTimeMillis }
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.add(java.util.Calendar.DAY_OF_YEAR, intervalDays)
            var delayMs = calendar.timeInMillis - System.currentTimeMillis()
            if (delayMs < 0) delayMs = 0
            val request = OneTimeWorkRequestBuilder<PackingReminderWorker>()
                .setInputData(input)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName(checklistId),
                ExistingWorkPolicy.REPLACE,
                request
            )
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "Rescheduled next run: checklistId=$checklistId in ${delayMs / 1000}s")
            }
        }
    }
}
