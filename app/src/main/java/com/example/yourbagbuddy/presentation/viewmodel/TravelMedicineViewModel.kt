package com.example.yourbagbuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.domain.model.MedicineCategory
import com.example.yourbagbuddy.domain.model.TravelMedicine
import com.example.yourbagbuddy.domain.usecase.medicine.GetAllMedicinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TravelMedicineViewModel @Inject constructor(
    private val getAllMedicinesUseCase: GetAllMedicinesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TravelMedicineUiState())
    val uiState: StateFlow<TravelMedicineUiState> = _uiState.asStateFlow()

    init {
        loadMedicines()
    }

    private fun loadMedicines() {
        viewModelScope.launch {
            getAllMedicinesUseCase().collect { medicines ->
                val byCategory = medicines.groupBy { it.category }
                _uiState.value = _uiState.value.copy(
                    medicinesByCategory = byCategory,
                    isLoading = false
                )
            }
        }
    }
}

data class TravelMedicineUiState(
    val medicinesByCategory: Map<MedicineCategory, List<TravelMedicine>> = emptyMap(),
    val isLoading: Boolean = true
)
