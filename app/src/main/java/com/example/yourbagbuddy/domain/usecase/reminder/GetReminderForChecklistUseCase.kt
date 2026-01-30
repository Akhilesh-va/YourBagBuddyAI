package com.example.yourbagbuddy.domain.usecase.reminder

import com.example.yourbagbuddy.domain.model.Reminder
import com.example.yourbagbuddy.domain.repository.ReminderRepository
import javax.inject.Inject

/**
 * Load reminder for a checklist (trip). Used by UI to show current reminder settings.
 */
class GetReminderForChecklistUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(checklistId: String): Reminder? =
        reminderRepository.getReminderByChecklistId(checklistId)
}
