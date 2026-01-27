package com.example.yourbagbuddy.domain.usecase.trip

import com.example.yourbagbuddy.domain.repository.TripRepository
import javax.inject.Inject

class DeleteTripUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String): Result<Unit> {
        return tripRepository.deleteTrip(tripId)
    }
}
