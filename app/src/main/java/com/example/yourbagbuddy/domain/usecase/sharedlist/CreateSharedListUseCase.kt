package com.example.yourbagbuddy.domain.usecase.sharedlist

import com.example.yourbagbuddy.domain.model.SharedList
import com.example.yourbagbuddy.domain.repository.SharedListRepository
import java.util.UUID
import javax.inject.Inject

class CreateSharedListUseCase @Inject constructor(
    private val sharedListRepository: SharedListRepository
) {
    /**
     * Generates a 6-character uppercase alphanumeric invite code.
     */
    fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    suspend operator fun invoke(
        name: String,
        tripId: String?,
        ownerId: String
    ): Result<Pair<SharedList, String>> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("List name cannot be empty"))
        }
        if (ownerId.isBlank()) {
            return Result.failure(IllegalArgumentException("Owner ID is required"))
        }
        val listId = UUID.randomUUID().toString()
        val inviteCode = generateInviteCode()
        return sharedListRepository.createSharedList(
            listId = listId,
            name = name,
            tripId = tripId,
            ownerId = ownerId,
            inviteCode = inviteCode
        ).map {
            Pair(
                SharedList(
                    id = listId,
                    name = name,
                    tripId = tripId,
                    ownerId = ownerId,
                    memberIds = listOf(ownerId),
                    inviteCode = inviteCode
                ),
                inviteCode
            )
        }
    }
}
