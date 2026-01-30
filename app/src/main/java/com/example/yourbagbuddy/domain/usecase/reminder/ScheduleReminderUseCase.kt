package com.example.yourbagbuddy.domain.usecase.reminder

import com.example.yourbagbuddy.domain.model.RepeatType
import com.example.yourbagbuddy.domain.repository.ReminderRepository
import java.util.Date
import javax.inject.Inject

/**
 * Save reminder and schedule WorkManager work. Caller should request notification
 * permission before enabling reminders. Does not block UI on failure.
 */
class ScheduleReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(
        checklistId: String,
        reminderTime: Date,
        repeatType: RepeatType,
        repeatIntervalDays: Int?,
        isEnabled: Boolean,
        stopWhenCompleted: Boolean,
        stopAtTripStart: Boolean
    ): Result<Unit> =
        reminderRepository.saveReminder(
            checklistId = checklistId,
            reminderTime = reminderTime,
            repeatType = repeatType,
            repeatIntervalDays = repeatIntervalDays,
            isEnabled = isEnabled,
            stopWhenCompleted = stopWhenCompleted,
            stopAtTripStart = stopAtTripStart
        )
}
