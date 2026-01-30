package com.example.yourbagbuddy.domain.model

import java.util.Date

/**
 * Domain model for a packing reminder.
 * Pure Kotlin – no Android dependencies (Clean Architecture).
 *
 * checklistId in this app equals tripId (one checklist per trip).
 * Future-ready: can map to a separate checklist entity when backend sync is added.
 */
data class Reminder(
    val reminderId: String,
    val checklistId: String,
    val reminderTime: Date,
    val repeatType: RepeatType,
    val repeatIntervalDays: Int?,
    val isEnabled: Boolean,
    val stopWhenCompleted: Boolean,
    val stopAtTripStart: Boolean,
    val createdAt: Date
)

/**
 * How often the reminder repeats.
 * ONCE = single notification at reminderTime.
 * DAILY = every day at same time.
 * EVERY_X_DAYS = every N days (use repeatIntervalDays).
 */
enum class RepeatType {
    ONCE,
    DAILY,
    EVERY_X_DAYS
}
