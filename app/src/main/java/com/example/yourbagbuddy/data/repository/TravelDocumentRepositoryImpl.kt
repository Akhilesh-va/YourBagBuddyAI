package com.example.yourbagbuddy.data.repository

import com.example.yourbagbuddy.data.local.dao.TravelDocumentDao
import com.example.yourbagbuddy.data.local.entity.TravelDocumentEntity
import com.example.yourbagbuddy.domain.model.TravelDocument
import com.example.yourbagbuddy.domain.model.TravelDocumentType
import com.example.yourbagbuddy.domain.repository.TravelDocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TravelDocumentRepositoryImpl @Inject constructor(
    private val travelDocumentDao: TravelDocumentDao
) : TravelDocumentRepository {

    override fun getDocumentsForTrip(tripId: String): Flow<List<TravelDocument>> {
        return travelDocumentDao.getDocumentsForTrip(tripId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun enableDocumentsChecklist(tripId: String): Result<Unit> {
        return try {
            val defaults = TravelDocumentType.defaultDocumentsForTrip(tripId)
            travelDocumentDao.insertAll(defaults.map { TravelDocumentEntity.fromDomain(it) })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleDocumentChecked(id: String, isChecked: Boolean): Result<Unit> {
        return try {
            travelDocumentDao.setChecked(id, isChecked)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
