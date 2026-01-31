package com.example.yourbagbuddy.domain.usecase.sharedlist

import com.example.yourbagbuddy.domain.repository.SharedListRepository
import javax.inject.Inject

class DeleteSharedListItemUseCase @Inject constructor(
    private val sharedListRepository: SharedListRepository
) {
    suspend operator fun invoke(listId: String, itemId: String): Result<Unit> {
        return sharedListRepository.deleteSharedListItem(listId, itemId)
    }
}
