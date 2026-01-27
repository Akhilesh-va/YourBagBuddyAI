package com.example.yourbagbuddy.domain.model

data class TravelMedicine(
    val id: String,
    val name: String,
    val category: MedicineCategory,
    val description: String
)

enum class MedicineCategory {
    FEVER_PAIN,
    STOMACH_ISSUES,
    MOTION_SICKNESS,
    FIRST_AID,
    DEHYDRATION
}
