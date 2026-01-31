package com.example.yourbagbuddy.domain.usecase.trip

import com.example.yourbagbuddy.domain.usecase.checklist.AddChecklistItemUseCase
import com.example.yourbagbuddy.domain.usecase.checklist.GetChecklistItemsUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DuplicateTripUseCase @Inject constructor(
    private val getTripByIdUseCase: GetTripByIdUseCase,
    private val createTripUseCase: CreateTripUseCase,
    private val getChecklistItemsUseCase: GetChecklistItemsUseCase,
    private val addChecklistItemUseCase: AddChecklistItemUseCase
) {
    suspend operator fun invoke(tripId: String): Result<String> {
        val trip = getTripByIdUseCase(tripId).first() ?: return Result.failure(IllegalArgumentException("Trip not found"))
        val copyName = "${trip.name} (copy)"
        val createResult = createTripUseCase(
            name = copyName,
            destination = trip.destination,
            startDate = trip.startDate,
            endDate = trip.endDate,
            numberOfPeople = trip.numberOfPeople,
            tripType = trip.tripType,
            userId = trip.userId
        )
        val newTripId = createResult.getOrElse { return createResult }
        val items = getChecklistItemsUseCase(tripId).first()
        for (item in items) {
            addChecklistItemUseCase(newTripId, item.name, item.category).getOrElse {
                // Continue copying other items even if one fails
            }
        }
        return Result.success(newTripId)
    }
}
