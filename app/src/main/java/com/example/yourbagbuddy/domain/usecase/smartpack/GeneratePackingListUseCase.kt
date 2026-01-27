package com.example.yourbagbuddy.domain.usecase.smartpack

import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.model.SmartPackRequest
import com.example.yourbagbuddy.domain.repository.SmartPackRepository
import javax.inject.Inject

class GeneratePackingListUseCase @Inject constructor(
    private val smartPackRepository: SmartPackRepository
) {
    suspend operator fun invoke(request: SmartPackRequest): Result<List<ChecklistItem>> {
        return smartPackRepository.generatePackingList(request)
    }
}
