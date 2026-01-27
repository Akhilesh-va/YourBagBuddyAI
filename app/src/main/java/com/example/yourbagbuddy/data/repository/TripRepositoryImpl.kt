package com.example.yourbagbuddy.data.repository

import com.example.yourbagbuddy.data.local.dao.TripDao
import com.example.yourbagbuddy.data.local.entity.TripEntity
import com.example.yourbagbuddy.data.remote.firebase.FirebaseTripDataSource
import com.example.yourbagbuddy.domain.model.Trip
import com.example.yourbagbuddy.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao,
    private val firebaseTripDataSource: FirebaseTripDataSource
) : TripRepository {
    
    override fun getAllTrips(userId: String?): Flow<List<Trip>> {
        return tripDao.getAllTrips(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getTripById(tripId: String): Flow<Trip?> {
        return tripDao.getTripById(tripId).map { it?.toDomain() }
    }
    
    override suspend fun createTrip(trip: Trip): Result<String> {
        return try {
            val entity = TripEntity.fromDomain(trip)
            tripDao.insertTrip(entity)
            Result.success(trip.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateTrip(trip: Trip): Result<Unit> {
        return try {
            val entity = TripEntity.fromDomain(trip).copy(lastSyncedAt = null)
            tripDao.updateTrip(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            tripDao.deleteTripById(tripId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun syncTrips(userId: String): Result<Unit> {
        return try {
            // Get local trips
            val localTrips = tripDao.getTripsByUserId(userId)
            
            // Sync to Firebase
            val syncResult = firebaseTripDataSource.syncTrips(userId, localTrips)
            if (syncResult.isFailure) {
                return syncResult
            }
            
            // Get trips from Firebase
            val firestoreResult = firebaseTripDataSource.getTripsFromFirestore(userId)
            if (firestoreResult.isSuccess) {
                val firestoreTrips = firestoreResult.getOrNull() ?: emptyList()
                
                // Update local database with Firestore trips (last-write-wins)
                firestoreTrips.forEach { firestoreTrip ->
                    val localTrip = localTrips.find { it.id == firestoreTrip.id }
                    if (localTrip == null || 
                        firestoreTrip.lastSyncedAt != null &&
                        (localTrip.lastSyncedAt == null || 
                         firestoreTrip.lastSyncedAt!! > localTrip.lastSyncedAt!!)) {
                        tripDao.insertTrip(firestoreTrip)
                    }
                }
            }
            
            // Update lastSyncedAt for synced trips
            val now = Date().time
            localTrips.forEach { trip ->
                tripDao.updateTrip(trip.copy(lastSyncedAt = now))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
