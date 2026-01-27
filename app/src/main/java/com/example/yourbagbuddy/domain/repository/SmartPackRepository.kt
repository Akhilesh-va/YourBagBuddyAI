package com.example.yourbagbuddy.domain.repository

import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.model.SmartPackRequest

interface SmartPackRepository {
    suspend fun generatePackingList(request: SmartPackRequest): Result<List<ChecklistItem>>
}
