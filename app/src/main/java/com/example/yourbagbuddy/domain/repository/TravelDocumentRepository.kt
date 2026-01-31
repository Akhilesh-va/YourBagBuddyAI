package com.example.yourbagbuddy.domain.repository

import com.example.yourbagbuddy.domain.model.TravelDocument
import kotlinx.coroutines.flow.Flow

interface TravelDocumentRepository {
    fun getDocumentsForTrip(tripId: String): Flow<List<TravelDocument>>
    suspend fun enableDocumentsChecklist(tripId: String): Result<Unit>
    suspend fun toggleDocumentChecked(id: String, isChecked: Boolean): Result<Unit>
}
