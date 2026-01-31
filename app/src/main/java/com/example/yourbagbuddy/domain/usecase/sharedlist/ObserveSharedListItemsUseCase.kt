package com.example.yourbagbuddy.domain.usecase.sharedlist

import com.example.yourbagbuddy.domain.model.SharedListItem
import com.example.yourbagbuddy.domain.repository.SharedListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSharedListItemsUseCase @Inject constructor(
    private val sharedListRepository: SharedListRepository
) {
    operator fun invoke(listId: String): Flow<List<SharedListItem>> {
        return sharedListRepository.observeSharedListItems(listId)
    }
}
