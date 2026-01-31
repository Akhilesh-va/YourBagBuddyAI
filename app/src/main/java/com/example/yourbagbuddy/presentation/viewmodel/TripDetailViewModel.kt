package com.example.yourbagbuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.model.Trip
import com.example.yourbagbuddy.domain.usecase.checklist.AddChecklistItemUseCase
import com.example.yourbagbuddy.domain.usecase.checklist.GetChecklistItemsUseCase
import com.example.yourbagbuddy.domain.usecase.checklist.ToggleItemPackedUseCase
import com.example.yourbagbuddy.domain.usecase.trip.DuplicateTripUseCase
import com.example.yourbagbuddy.domain.usecase.trip.GetTripByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val getChecklistItemsUseCase: GetChecklistItemsUseCase,
    private val addChecklistItemUseCase: AddChecklistItemUseCase,
    private val toggleItemPackedUseCase: ToggleItemPackedUseCase,
    private val getTripByIdUseCase: GetTripByIdUseCase,
    private val duplicateTripUseCase: DuplicateTripUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    fun loadChecklist(tripId: String) {
        viewModelScope.launch {
            val trip = getTripByIdUseCase(tripId).first()
            _uiState.value = _uiState.value.copy(trip = trip)
        }
        viewModelScope.launch {
            getChecklistItemsUseCase(tripId).collect { items ->
                val packedCount = items.count { it.isPacked }
                val totalCount = items.size
                _uiState.value = _uiState.value.copy(
                    items = items,
                    packedCount = packedCount,
                    totalCount = totalCount,
                    progress = if (totalCount > 0) (packedCount.toFloat() / totalCount) else 0f
                )
            }
        }
    }
    
    fun addItem(tripId: String, name: String, category: com.example.yourbagbuddy.domain.model.ItemCategory) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = addChecklistItemUseCase(tripId, name, category)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to add item"
                    )
                }
            )
        }
    }
    
    fun toggleItemPacked(itemId: String, isPacked: Boolean) {
        viewModelScope.launch {
            toggleItemPackedUseCase(itemId, isPacked)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    suspend fun duplicateTrip(tripId: String): Result<String> {
        return duplicateTripUseCase(tripId)
    }
}

data class TripDetailUiState(
    val trip: Trip? = null,
    val items: List<ChecklistItem> = emptyList(),
    val packedCount: Int = 0,
    val totalCount: Int = 0,
    val progress: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null
)
