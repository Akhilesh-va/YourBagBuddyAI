package com.example.yourbagbuddy.domain.usecase.traveldocument

import com.example.yourbagbuddy.domain.repository.TravelDocumentRepository
import javax.inject.Inject

class EnableDocumentsChecklistUseCase @Inject constructor(
    private val travelDocumentRepository: TravelDocumentRepository
) {
    suspend operator fun invoke(tripId: String) =
        travelDocumentRepository.enableDocumentsChecklist(tripId)
}
