package com.example.yourbagbuddy.domain.model

/**
 * A shared checklist that multiple users can edit from their own devices.
 * Items are stored in Firestore under sharedLists/{listId}/checklistItems.
 */
data class SharedList(
    val id: String,
    val name: String,
    val tripId: String?,
    val ownerId: String,
    val memberIds: List<String>,
    val inviteCode: String
)

/**
 * Checklist item in a shared list; may include who added it for display.
 */
data class SharedListItem(
    val id: String,
    val listId: String,
    val name: String,
    val category: ItemCategory,
    val isPacked: Boolean,
    val addedByUserId: String? = null
) {
    fun toChecklistItem(tripId: String): ChecklistItem = ChecklistItem(
        id = id,
        tripId = tripId,
        name = name,
        category = category,
        isPacked = isPacked
    )

    companion object {
        fun fromChecklistItem(item: ChecklistItem, listId: String, addedByUserId: String? = null) =
            SharedListItem(
                id = item.id,
                listId = listId,
                name = item.name,
                category = item.category,
                isPacked = item.isPacked,
                addedByUserId = addedByUserId
            )
    }
}
