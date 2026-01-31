package com.example.yourbagbuddy.domain.usecase.sharedlist

import com.example.yourbagbuddy.domain.repository.SharedListRepository
import javax.inject.Inject

class LeaveSharedListUseCase @Inject constructor(
    private val sharedListRepository: SharedListRepository
) {
    suspend operator fun invoke(listId: String, userId: String): Result<Unit> {
        if (listId.isBlank()) {
            return Result.failure(IllegalArgumentException("List ID cannot be empty"))
        }
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID is required"))
        }
        return sharedListRepository.leaveSharedList(listId, userId)
    }
}
