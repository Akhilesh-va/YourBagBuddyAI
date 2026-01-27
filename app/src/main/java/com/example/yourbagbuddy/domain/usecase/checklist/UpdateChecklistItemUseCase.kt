package com.example.yourbagbuddy.domain.usecase.checklist

import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.repository.ChecklistRepository
import javax.inject.Inject

class UpdateChecklistItemUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(item: ChecklistItem): Result<Unit> {
        if (item.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Item name cannot be empty"))
        }
        return checklistRepository.updateChecklistItem(item)
    }
}
