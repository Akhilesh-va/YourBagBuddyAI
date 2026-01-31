package com.example.yourbagbuddy.domain.usecase.traveldocument

import com.example.yourbagbuddy.domain.repository.TravelDocumentRepository
import javax.inject.Inject

class ToggleTravelDocumentCheckedUseCase @Inject constructor(
    private val travelDocumentRepository: TravelDocumentRepository
) {
    suspend operator fun invoke(documentId: String, isChecked: Boolean) =
        travelDocumentRepository.toggleDocumentChecked(documentId, isChecked)
}
