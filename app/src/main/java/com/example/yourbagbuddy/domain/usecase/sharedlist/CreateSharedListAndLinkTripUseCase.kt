package com.example.yourbagbuddy.domain.usecase.sharedlist

import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.model.SharedListItem
import com.example.yourbagbuddy.domain.repository.SharedListRepository
import com.example.yourbagbuddy.domain.repository.TripRepository
import com.example.yourbagbuddy.domain.usecase.trip.UpdateTripUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Creates a shared list for a trip, copies current checklist items into it,
 * and links the trip to the shared list so owner and members all see the same list.
 */
class CreateSharedListAndLinkTripUseCase @Inject constructor(
    private val createSharedListUseCase: CreateSharedListUseCase,
    private val sharedListRepository: SharedListRepository,
    private val tripRepository: TripRepository,
    private val updateTripUseCase: UpdateTripUseCase
) {
    suspend operator fun invoke(
        tripId: String,
        listName: String,
        currentItems: List<ChecklistItem>,
        ownerId: String
    ): Result<Pair<String, String>> {
        if (listName.isBlank()) {
            return Result.failure(IllegalArgumentException("List name cannot be empty"))
        }
        if (ownerId.isBlank()) {
            return Result.failure(IllegalArgumentException("Owner ID is required"))
        }
        val trip = tripRepository.getTripById(tripId).first() ?: return Result.failure(IllegalArgumentException("Trip not found"))
        val createResult = createSharedListUseCase(
            name = listName,
            tripId = tripId,
            ownerId = ownerId
        )
        val (sharedList, inviteCode) = createResult.getOrElse { return Result.failure(it) }
        currentItems.forEach { item ->
            val sharedItem = SharedListItem.fromChecklistItem(item, sharedList.id, ownerId)
            sharedListRepository.addSharedListItem(sharedList.id, sharedItem)
        }
        val updateResult = updateTripUseCase(trip.copy(sharedListId = sharedList.id))
        return updateResult.map { Pair(sharedList.id, inviteCode) }
    }
}
