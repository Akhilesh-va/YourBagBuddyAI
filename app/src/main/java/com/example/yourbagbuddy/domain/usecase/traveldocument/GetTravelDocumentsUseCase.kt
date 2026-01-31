package com.example.yourbagbuddy.domain.usecase.traveldocument

import com.example.yourbagbuddy.domain.model.TravelDocument
import com.example.yourbagbuddy.domain.repository.TravelDocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTravelDocumentsUseCase @Inject constructor(
    private val travelDocumentRepository: TravelDocumentRepository
) {
    operator fun invoke(tripId: String): Flow<List<TravelDocument>> {
        return travelDocumentRepository.getDocumentsForTrip(tripId)
    }
}
