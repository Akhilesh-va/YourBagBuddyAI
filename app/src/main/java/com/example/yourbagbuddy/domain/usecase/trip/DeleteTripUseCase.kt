package com.example.yourbagbuddy.domain.usecase.trip

import com.example.yourbagbuddy.domain.repository.ChecklistRepository
import com.example.yourbagbuddy.domain.repository.TripRepository
import com.example.yourbagbuddy.domain.usecase.reminder.CancelReminderUseCase
import javax.inject.Inject

class DeleteTripUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val checklistRepository: ChecklistRepository,
    private val cancelReminderUseCase: CancelReminderUseCase
) {
    /**
     * Deletes the trip, syncs checklist item deletion to remote, and cancels any reminder.
     */
    suspend operator fun invoke(tripId: String): Result<Unit> {
        cancelReminderUseCase(tripId)
        checklistRepository.deleteAllItemsForTrip(tripId)
        return tripRepository.deleteTrip(tripId)
    }
}
