package com.example.yourbagbuddy.domain.model

data class ChecklistItem(
    val id: String,
    val tripId: String,
    val name: String,
    val category: ItemCategory,
    val isPacked: Boolean
)

enum class ItemCategory {
    CLOTHES,
    ESSENTIALS,
    DOCUMENTS,
    OTHER
}
