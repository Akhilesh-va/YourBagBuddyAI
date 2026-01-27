package com.example.yourbagbuddy.domain.usecase.medicine

import com.example.yourbagbuddy.domain.model.TravelMedicine
import com.example.yourbagbuddy.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllMedicinesUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    operator fun invoke(): Flow<List<TravelMedicine>> {
        return medicineRepository.getAllMedicines()
    }
}
