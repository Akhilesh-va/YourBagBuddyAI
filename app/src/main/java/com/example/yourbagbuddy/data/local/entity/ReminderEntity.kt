package com.example.yourbagbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.yourbagbuddy.domain.model.Reminder
import com.example.yourbagbuddy.domain.model.RepeatType
import java.util.Date

/**
 * Room entity for packing reminders.
 * checklistId in this app = tripId (one checklist per trip).
 * Foreign key to trips: when trip is deleted, reminder is orphaned; we cancel WorkManager on delete.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["checklistId"], unique = true)]
)
data class ReminderEntity(
    @PrimaryKey
    val reminderId: String,
    val checklistId: String,
    val reminderTime: Long,
    val repeatType: String,
    val repeatIntervalDays: Int?,
    val isEnabled: Boolean,
    val stopWhenCompleted: Boolean,
    val stopAtTripStart: Boolean,
    val createdAt: Long
) {
    fun toDomain(): Reminder {
        return Reminder(
            reminderId = reminderId,
            checklistId = checklistId,
            reminderTime = Date(reminderTime),
            repeatType = RepeatType.valueOf(repeatType),
            repeatIntervalDays = repeatIntervalDays,
            isEnabled = isEnabled,
            stopWhenCompleted = stopWhenCompleted,
            stopAtTripStart = stopAtTripStart,
            createdAt = Date(createdAt)
        )
    }

    companion object {
        fun fromDomain(r: Reminder): ReminderEntity {
            return ReminderEntity(
                reminderId = r.reminderId,
                checklistId = r.checklistId,
                reminderTime = r.reminderTime.time,
                repeatType = r.repeatType.name,
                repeatIntervalDays = r.repeatIntervalDays,
                isEnabled = r.isEnabled,
                stopWhenCompleted = r.stopWhenCompleted,
                stopAtTripStart = r.stopAtTripStart,
                createdAt = r.createdAt.time
            )
        }
    }
}
