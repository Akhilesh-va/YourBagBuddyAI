package com.example.yourbagbuddy.domain.repository

import com.example.yourbagbuddy.domain.model.ChecklistItem
import kotlinx.coroutines.flow.Flow

interface ChecklistRepository {
    fun getChecklistItems(tripId: String): Flow<List<ChecklistItem>>
    suspend fun addChecklistItem(item: ChecklistItem): Result<Unit>
    suspend fun updateChecklistItem(item: ChecklistItem): Result<Unit>
    suspend fun deleteChecklistItem(itemId: String): Result<Unit>
    suspend fun deleteAllItemsForTrip(tripId: String): Result<Unit>
    suspend fun toggleItemPacked(itemId: String, isPacked: Boolean): Result<Unit>
}
