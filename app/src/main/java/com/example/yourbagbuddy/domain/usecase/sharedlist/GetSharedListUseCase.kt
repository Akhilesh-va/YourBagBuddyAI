package com.example.yourbagbuddy.domain.usecase.sharedlist

import com.example.yourbagbuddy.domain.model.SharedList
import com.example.yourbagbuddy.domain.repository.SharedListRepository
import javax.inject.Inject

class GetSharedListUseCase @Inject constructor(
    private val sharedListRepository: SharedListRepository
) {
    suspend operator fun invoke(listId: String): Result<SharedList?> {
        return sharedListRepository.getSharedList(listId)
    }
}
