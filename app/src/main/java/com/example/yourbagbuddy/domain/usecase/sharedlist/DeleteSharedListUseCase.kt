package com.example.yourbagbuddy.domain.usecase.sharedlist

import com.example.yourbagbuddy.domain.repository.SharedListRepository
import javax.inject.Inject

/**
 * Deletes a shared list (Firestore document). When the owner deletes their checklist
 * that is linked to this shared list, call this so the list disappears for all joined members.
 */
class DeleteSharedListUseCase @Inject constructor(
    private val sharedListRepository: SharedListRepository
) {
    suspend operator fun invoke(listId: String): Result<Unit> {
        if (listId.isBlank()) {
            return Result.failure(IllegalArgumentException("List ID cannot be empty"))
        }
        return sharedListRepository.deleteSharedList(listId)
    }
}
