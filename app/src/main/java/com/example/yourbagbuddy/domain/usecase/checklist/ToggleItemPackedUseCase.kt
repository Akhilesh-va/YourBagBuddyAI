package com.example.yourbagbuddy.domain.usecase.checklist

import com.example.yourbagbuddy.domain.repository.ChecklistRepository
import javax.inject.Inject

class ToggleItemPackedUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(itemId: String, isPacked: Boolean): Result<Unit> {
        return checklistRepository.toggleItemPacked(itemId, isPacked)
    }
}
