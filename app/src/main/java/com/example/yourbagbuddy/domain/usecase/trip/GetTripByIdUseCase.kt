package com.example.yourbagbuddy.domain.usecase.trip

import com.example.yourbagbuddy.domain.model.Trip
import com.example.yourbagbuddy.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTripByIdUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    operator fun invoke(tripId: String): Flow<Trip?> {
        return tripRepository.getTripById(tripId)
    }
}
