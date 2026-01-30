package com.example.yourbagbuddy.domain.usecase.reminder

import com.example.yourbagbuddy.domain.repository.ReminderRepository
import javax.inject.Inject

/**
 * Cancel reminder for a checklist: cancel WorkManager work and disable reminder in DB.
 * Called when user disables reminders, checklist/trip is deleted, or stop conditions met.
 */
class CancelReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(checklistId: String) {
        reminderRepository.cancelReminderForChecklist(checklistId)
    }
}
