package com.example.yourbagbuddy.domain.usecase.sharedlist

import com.example.yourbagbuddy.domain.model.SharedListItem
import com.example.yourbagbuddy.domain.repository.SharedListRepository
import javax.inject.Inject

class ToggleSharedListItemPackedUseCase @Inject constructor(
    private val sharedListRepository: SharedListRepository
) {
    suspend operator fun invoke(listId: String, item: SharedListItem, isPacked: Boolean): Result<Unit> {
        val updated = item.copy(isPacked = isPacked)
        return sharedListRepository.updateSharedListItem(listId, updated)
    }
}
