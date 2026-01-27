package com.example.yourbagbuddy.domain.usecase.trip

import com.example.yourbagbuddy.domain.model.Trip
import com.example.yourbagbuddy.domain.repository.TripRepository
import javax.inject.Inject

class UpdateTripUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(trip: Trip): Result<Unit> {
        return tripRepository.updateTrip(trip)
    }
}
