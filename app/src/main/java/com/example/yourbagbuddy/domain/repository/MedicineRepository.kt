package com.example.yourbagbuddy.domain.repository

import com.example.yourbagbuddy.domain.model.TravelMedicine
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    fun getAllMedicines(): Flow<List<TravelMedicine>>
    fun getMedicinesByCategory(category: com.example.yourbagbuddy.domain.model.MedicineCategory): Flow<List<TravelMedicine>>
}
