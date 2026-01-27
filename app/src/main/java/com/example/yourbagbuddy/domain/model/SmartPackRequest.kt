package com.example.yourbagbuddy.domain.model

import java.util.Date

data class SmartPackRequest(
    val destination: String,
    val month: String,
    val tripDuration: Int, // in days
    val numberOfPeople: Int,
    val tripType: TripType
)
