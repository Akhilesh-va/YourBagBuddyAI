package com.example.yourbagbuddy.domain.usecase.sharedlist

import com.example.yourbagbuddy.domain.model.SharedList
import com.example.yourbagbuddy.domain.repository.SharedListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSharedListsForUserUseCase @Inject constructor(
    private val sharedListRepository: SharedListRepository
) {
    operator fun invoke(userId: String): Flow<List<SharedList>> {
        return sharedListRepository.getSharedListsForUser(userId)
    }
}
