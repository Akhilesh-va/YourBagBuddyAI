package com.example.yourbagbuddy.domain.usecase.trip

import com.example.yourbagbuddy.domain.model.Trip
import com.example.yourbagbuddy.domain.repository.TripRepository
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class CreateTripUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(
        name: String,
        destination: String,
        startDate: Date,
        endDate: Date,
        numberOfPeople: Int,
        tripType: com.example.yourbagbuddy.domain.model.TripType,
        userId: String?
    ): Result<String> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Trip name cannot be empty"))
        }
        if (destination.isBlank()) {
            return Result.failure(IllegalArgumentException("Destination cannot be empty"))
        }
        if (startDate.after(endDate)) {
            return Result.failure(IllegalArgumentException("Start date must be before end date"))
        }
        
        val trip = Trip(
            id = UUID.randomUUID().toString(),
            name = name,
            destination = destination,
            startDate = startDate,
            endDate = endDate,
            numberOfPeople = numberOfPeople,
            tripType = tripType,
            createdDate = Date(),
            userId = userId
        )
        
        return tripRepository.createTrip(trip)
    }
}
