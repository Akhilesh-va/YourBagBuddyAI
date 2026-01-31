package com.example.yourbagbuddy.domain.usecase.sharedlist

import com.example.yourbagbuddy.domain.model.SharedList
import com.example.yourbagbuddy.domain.repository.SharedListRepository
import javax.inject.Inject

class JoinSharedListUseCase @Inject constructor(
    private val sharedListRepository: SharedListRepository
) {
    suspend operator fun invoke(inviteCode: String, userId: String): Result<SharedList> {
        if (inviteCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Invite code cannot be empty"))
        }
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID is required"))
        }
        return sharedListRepository.addMemberByInviteCode(inviteCode, userId).map { list ->
            requireNotNull(list) { "Shared list not found" }
        }
    }
}
