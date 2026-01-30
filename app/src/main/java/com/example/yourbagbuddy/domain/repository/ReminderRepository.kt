package com.example.yourbagbuddy.domain.repository

import com.example.yourbagbuddy.domain.model.Reminder
import com.example.yourbagbuddy.domain.model.RepeatType
import java.util.Date

/**
 * Repository for reminder persistence and scheduling.
 * Implementations use Room + WorkManager; domain layer stays framework-agnostic.
 * Future-ready: implementation can switch to backend sync / push without changing this interface.
 */
interface ReminderRepository {

    /**
     * Get the reminder for a checklist (trip), if any.
     */
    suspend fun getReminderByChecklistId(checklistId: String): Reminder?

    /**
     * Save reminder and schedule WorkManager work.
     * Replaces any existing reminder for this checklistId (UniqueWork).
     * Caller should request notification permission before enabling reminders.
     *
     * @return Result.success(Unit) if saved and scheduled (or disabled); failure on DB/WorkManager error.
     */
    suspend fun saveReminder(
        checklistId: String,
        reminderTime: Date,
        repeatType: RepeatType,
        repeatIntervalDays: Int?,
        isEnabled: Boolean,
        stopWhenCompleted: Boolean,
        stopAtTripStart: Boolean
    ): Result<Unit>

    /**
     * Cancel all scheduled work for this checklist and optionally disable the reminder in DB.
     * Called when: user disables reminder, checklist/trip deleted, or stop conditions met.
     */
    suspend fun cancelReminderForChecklist(checklistId: String)

    /**
     * Get trip start date for a checklist (checklistId = tripId). Used for stop condition.
     */
    suspend fun getTripStartTimeMillis(checklistId: String): Long?
}
