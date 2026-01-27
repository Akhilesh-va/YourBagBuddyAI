package com.example.yourbagbuddy.domain.model

import java.util.Date

data class Trip(
    val id: String,
    val name: String,
    val destination: String,
    val startDate: Date,
    val endDate: Date,
    val numberOfPeople: Int,
    val tripType: TripType,
    val createdDate: Date,
    val userId: String? = null // null for guest users
)

enum class TripType {
    VACATION,
    WORK,
    TREK,
    WEDDING
}
