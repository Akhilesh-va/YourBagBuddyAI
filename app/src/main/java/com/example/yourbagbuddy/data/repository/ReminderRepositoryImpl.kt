package com.example.yourbagbuddy.data.repository

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.yourbagbuddy.data.local.dao.ReminderDao
import com.example.yourbagbuddy.data.local.dao.TripDao
import com.example.yourbagbuddy.domain.model.Reminder
import com.example.yourbagbuddy.domain.model.RepeatType
import com.example.yourbagbuddy.domain.repository.ReminderRepository
import com.example.yourbagbuddy.worker.PackingReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Implementation of ReminderRepository using Room + WorkManager.
 * Scheduling logic lives here (data layer); UI and ViewModel never touch WorkManager.
 * Future-ready: can swap to backend sync / push without changing domain or UI.
 */
class ReminderRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao,
    private val tripDao: TripDao
) : ReminderRepository {

    override suspend fun getReminderByChecklistId(checklistId: String): Reminder? {
        return try {
            reminderDao.getByChecklistId(checklistId)?.toDomain()
        } catch (e: Exception) {
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.w(TAG, "getReminderByChecklistId failed", e)
            }
            null
        }
    }

    override suspend fun saveReminder(
        checklistId: String,
        reminderTime: Date,
        repeatType: RepeatType,
        repeatIntervalDays: Int?,
        isEnabled: Boolean,
        stopWhenCompleted: Boolean,
        stopAtTripStart: Boolean
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val existing = reminderDao.getByChecklistId(checklistId)
            val reminderId = existing?.reminderId ?: UUID.randomUUID().toString()
            val entity = com.example.yourbagbuddy.data.local.entity.ReminderEntity(
                reminderId = reminderId,
                checklistId = checklistId,
                reminderTime = reminderTime.time,
                repeatType = repeatType.name,
                repeatIntervalDays = repeatIntervalDays,
                isEnabled = isEnabled,
                stopWhenCompleted = stopWhenCompleted,
                stopAtTripStart = stopAtTripStart,
                createdAt = existing?.createdAt ?: now
            )
            reminderDao.insert(entity)

            if (isEnabled) {
                scheduleWork(checklistId, reminderTime, repeatType, repeatIntervalDays)
            } else {
                cancelReminderForChecklist(checklistId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.w(TAG, "saveReminder failed", e)
            }
            Result.failure(e)
        }
    }

    override suspend fun cancelReminderForChecklist(checklistId: String) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(checklistId))
            // Optionally disable in DB so UI shows "off"; we keep record for user to re-enable.
            reminderDao.getByChecklistId(checklistId)?.let { entity ->
                reminderDao.insert(entity.copy(isEnabled = false))
            }
        } catch (e: Exception) {
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.w(TAG, "cancelReminderForChecklist failed", e)
            }
        }
    }

    override suspend fun getTripStartTimeMillis(checklistId: String): Long? {
        return try {
            tripDao.getTripByIdOnce(checklistId)?.startDate
        } catch (e: Exception) {
            if (com.example.yourbagbuddy.BuildConfig.DEBUG) {
                android.util.Log.w(TAG, "getTripStartTimeMillis failed", e)
            }
            null
        }
    }

    /**
     * Schedules one-time work at the exact reminder time. Worker will show notification only if
     * unchecked items exist and will reschedule for DAILY/EVERY_X_DAYS (Worker reads repeat config from DB).
     */
    private fun scheduleWork(
        checklistId: String,
        reminderTime: Date,
        @Suppress("UNUSED_PARAMETER") repeatType: RepeatType,
        @Suppress("UNUSED_PARAMETER") repeatIntervalDays: Int?
    ) {
        val input = Data.Builder()
            .putString(PackingReminderWorker.KEY_CHECKLIST_ID, checklistId)
            .build()
        val triggerAtMs = reminderTime.time
        val delayMs = (triggerAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
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
            android.util.Log.d(TAG, "Scheduled reminder: checklistId=$checklistId triggerAt=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(reminderTime)} delayMs=$delayMs")
        }
    }

    companion object {
        private const val TAG = "ReminderRepo"
        const val REMINDER_UNIQUE_PREFIX = "packing_reminder_"

        fun uniqueWorkName(checklistId: String) = "${REMINDER_UNIQUE_PREFIX}$checklistId"
    }
}
