package com.example.yourbagbuddy.domain.usecase.checklist

import com.example.yourbagbuddy.domain.repository.ChecklistRepository
import javax.inject.Inject

class DeleteChecklistItemUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(itemId: String): Result<Unit> {
        return checklistRepository.deleteChecklistItem(itemId)
    }
}
