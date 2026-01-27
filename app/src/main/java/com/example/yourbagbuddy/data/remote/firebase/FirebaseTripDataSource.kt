package com.example.yourbagbuddy.data.remote.firebase

import com.example.yourbagbuddy.data.local.entity.TripEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseTripDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun syncTrips(userId: String, trips: List<TripEntity>): Result<Unit> {
        return try {
            val tripsCollection = firestore.collection("users").document(userId).collection("trips")
            
            // Get existing trips from Firestore
            val existingTrips = tripsCollection.get().await().documents.associate { 
                it.id to it.toObject(TripEntity::class.java)
            }
            
            // Upload new/updated trips
            trips.forEach { trip ->
                val existingTrip = existingTrips[trip.id]
                if (existingTrip == null || 
                    existingTrip.lastSyncedAt == null || 
                    trip.lastSyncedAt == null ||
                    trip.lastSyncedAt!! > existingTrip.lastSyncedAt!!) {
                    tripsCollection.document(trip.id).set(trip).await()
                }
            }
            
            // Download trips from Firestore that are newer
            existingTrips.forEach { (id, firestoreTrip) ->
                val localTrip = trips.find { it.id == id }
                if (localTrip == null || 
                    firestoreTrip?.lastSyncedAt != null &&
                    (localTrip.lastSyncedAt == null || firestoreTrip.lastSyncedAt!! > localTrip.lastSyncedAt!!)) {
                    // This trip should be updated locally (handled by repository)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTripsFromFirestore(userId: String): Result<List<TripEntity>> {
        return try {
            val trips = firestore.collection("users")
                .document(userId)
                .collection("trips")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(TripEntity::class.java) }
            Result.success(trips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
