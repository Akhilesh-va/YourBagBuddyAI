package com.example.yourbagbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.model.ItemCategory

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tripId"])]
)
data class ChecklistItemEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val name: String,
    val category: String,
    val isPacked: Boolean
) {
    fun toDomain(): ChecklistItem {
        return ChecklistItem(
            id = id,
            tripId = tripId,
            name = name,
            category = ItemCategory.valueOf(category),
            isPacked = isPacked
        )
    }
    
    companion object {
        fun fromDomain(item: ChecklistItem): ChecklistItemEntity {
            return ChecklistItemEntity(
                id = item.id,
                tripId = item.tripId,
                name = item.name,
                category = item.category.name,
                isPacked = item.isPacked
            )
        }
    }
}
