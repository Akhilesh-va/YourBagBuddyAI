package com.example.yourbagbuddy.domain.usecase.checklist

import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.repository.ChecklistRepository
import java.util.UUID
import javax.inject.Inject

class AddChecklistItemUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(
        tripId: String,
        name: String,
        category: com.example.yourbagbuddy.domain.model.ItemCategory
    ): Result<Unit> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Item name cannot be empty"))
        }
        
        val item = ChecklistItem(
            id = UUID.randomUUID().toString(),
            tripId = tripId,
            name = name,
            category = category,
            isPacked = false
        )
        
        return checklistRepository.addChecklistItem(item)
    }
}
