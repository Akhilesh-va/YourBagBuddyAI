package com.example.yourbagbuddy.domain.repository

import com.example.yourbagbuddy.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun getAllTrips(userId: String?): Flow<List<Trip>>
    fun getTripById(tripId: String): Flow<Trip?>
    suspend fun createTrip(trip: Trip): Result<String>
    suspend fun updateTrip(trip: Trip): Result<Unit>
    suspend fun deleteTrip(tripId: String): Result<Unit>
    suspend fun syncTrips(userId: String): Result<Unit>
}
