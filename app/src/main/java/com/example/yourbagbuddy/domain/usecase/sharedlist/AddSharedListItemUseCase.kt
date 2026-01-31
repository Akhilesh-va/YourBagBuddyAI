package com.example.yourbagbuddy.domain.usecase.sharedlist

import com.example.yourbagbuddy.domain.model.ItemCategory
import com.example.yourbagbuddy.domain.model.SharedListItem
import com.example.yourbagbuddy.domain.repository.SharedListRepository
import java.util.UUID
import javax.inject.Inject

class AddSharedListItemUseCase @Inject constructor(
    private val sharedListRepository: SharedListRepository
) {
    suspend operator fun invoke(
        listId: String,
        name: String,
        category: ItemCategory = ItemCategory.OTHER,
        addedByUserId: String?
    ): Result<Unit> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Item name cannot be empty"))
        }
        val item = SharedListItem(
            id = UUID.randomUUID().toString(),
            listId = listId,
            name = name.trim(),
            category = category,
            isPacked = false,
            addedByUserId = addedByUserId
        )
        return sharedListRepository.addSharedListItem(listId, item)
    }
}
