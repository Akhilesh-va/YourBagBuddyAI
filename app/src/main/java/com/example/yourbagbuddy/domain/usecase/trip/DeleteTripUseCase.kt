package com.example.yourbagbuddy.domain.usecase.trip

import com.example.yourbagbuddy.domain.repository.TripRepository
import com.example.yourbagbuddy.domain.usecase.reminder.CancelReminderUseCase
import javax.inject.Inject

class DeleteTripUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val cancelReminderUseCase: CancelReminderUseCase
) {
    /**
     * Deletes the trip and cancels any reminder for this checklist (tripId = checklistId).
     */
    suspend operator fun invoke(tripId: String): Result<Unit> {
        cancelReminderUseCase(tripId)
        return tripRepository.deleteTrip(tripId)
    }
}
