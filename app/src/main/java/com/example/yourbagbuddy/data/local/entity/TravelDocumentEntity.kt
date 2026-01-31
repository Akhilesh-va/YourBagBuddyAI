package com.example.yourbagbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.yourbagbuddy.domain.model.TravelDocument
import com.example.yourbagbuddy.domain.model.TravelDocumentType

@Entity(
    tableName = "travel_documents",
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
data class TravelDocumentEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val type: String,
    val displayName: String,
    val reminderText: String?,
    val resourceUrl: String?,
    val isChecked: Boolean
) {
    fun toDomain(): TravelDocument {
        return TravelDocument(
            id = id,
            tripId = tripId,
            type = TravelDocumentType.valueOf(type),
            displayName = displayName,
            reminderText = reminderText,
            resourceUrl = resourceUrl,
            isChecked = isChecked
        )
    }

    companion object {
        fun fromDomain(doc: TravelDocument): TravelDocumentEntity {
            return TravelDocumentEntity(
                id = doc.id,
                tripId = doc.tripId,
                type = doc.type.name,
                displayName = doc.displayName,
                reminderText = doc.reminderText,
                resourceUrl = doc.resourceUrl,
                isChecked = doc.isChecked
            )
        }
    }
}
