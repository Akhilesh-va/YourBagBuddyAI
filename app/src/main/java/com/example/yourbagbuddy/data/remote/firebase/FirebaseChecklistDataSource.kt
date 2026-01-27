package com.example.yourbagbuddy.data.remote.firebase

import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseChecklistDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun upsertChecklistItem(
        userId: String,
        tripId: String,
        item: ChecklistItem
    ): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("trips")
                .document(tripId)
                .collection("checklistItems")
                .document(item.id)
                .set(
                    mapOf(
                        "id" to item.id,
                        "tripId" to item.tripId,
                        "name" to item.name,
                        "category" to item.category.name,
                        "isPacked" to item.isPacked
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChecklistItem(
        userId: String,
        tripId: String,
        itemId: String
    ): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("trips")
                .document(tripId)
                .collection("checklistItems")
                .document(itemId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
