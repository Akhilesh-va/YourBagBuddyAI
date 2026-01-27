package com.example.yourbagbuddy.domain.usecase.checklist

import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.repository.ChecklistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChecklistItemsUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    operator fun invoke(tripId: String): Flow<List<ChecklistItem>> {
        return checklistRepository.getChecklistItems(tripId)
    }
}
