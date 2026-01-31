package com.example.yourbagbuddy.data.repository

import com.example.yourbagbuddy.data.local.dao.ChecklistItemDao
import com.example.yourbagbuddy.data.local.entity.ChecklistItemEntity
import com.example.yourbagbuddy.data.remote.firebase.FirebaseChecklistDataSource
import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.domain.repository.ChecklistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChecklistRepositoryImpl @Inject constructor(
    private val checklistItemDao: ChecklistItemDao,
    private val firebaseChecklistDataSource: FirebaseChecklistDataSource,
    private val authRepository: AuthRepository
) : ChecklistRepository {
    
    override fun getChecklistItems(tripId: String): Flow<List<ChecklistItem>> {
        return checklistItemDao.getChecklistItems(tripId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun addChecklistItem(item: ChecklistItem): Result<Unit> {
        return try {
            val entity = ChecklistItemEntity.fromDomain(item)
            checklistItemDao.insertItem(entity)
            syncChecklistItemToFirebase(item)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateChecklistItem(item: ChecklistItem): Result<Unit> {
        return try {
            val entity = ChecklistItemEntity.fromDomain(item)
            checklistItemDao.updateItem(entity)
            syncChecklistItemToFirebase(item)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteChecklistItem(itemId: String): Result<Unit> {
        return try {
            val entity = checklistItemDao.getItemById(itemId)
            checklistItemDao.deleteItemById(itemId)
            if (entity != null) {
                syncChecklistItemDeletionToFirebase(entity)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAllItemsForTrip(tripId: String): Result<Unit> {
        return try {
            val items = checklistItemDao.getChecklistItemsOnce(tripId)
            val userId = authRepository.getCurrentUser()?.id
            if (userId != null) {
                items.forEach { entity ->
                    firebaseChecklistDataSource.deleteChecklistItem(userId, tripId, entity.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun toggleItemPacked(itemId: String, isPacked: Boolean): Result<Unit> {
        return try {
            checklistItemDao.updatePackedStatus(itemId, isPacked)
            val entity = checklistItemDao.getItemById(itemId)
            if (entity != null) {
                val updatedItem = entity.toDomain().copy(isPacked = isPacked)
                syncChecklistItemToFirebase(updatedItem)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncChecklistItemToFirebase(item: ChecklistItem) {
        val userId = authRepository.getCurrentUser()?.id ?: return
        firebaseChecklistDataSource.upsertChecklistItem(
            userId = userId,
            tripId = item.tripId,
            item = item
        )
    }

    private suspend fun syncChecklistItemDeletionToFirebase(entity: ChecklistItemEntity) {
        val userId = authRepository.getCurrentUser()?.id ?: return
        firebaseChecklistDataSource.deleteChecklistItem(
            userId = userId,
            tripId = entity.tripId,
            itemId = entity.id
        )
    }
}
