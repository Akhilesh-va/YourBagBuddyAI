package com.example.yourbagbuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.model.SmartPackRequest
import com.example.yourbagbuddy.domain.model.TripType
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.domain.usecase.checklist.AddChecklistItemUseCase
import com.example.yourbagbuddy.domain.usecase.smartpack.GeneratePackingListUseCase
import com.example.yourbagbuddy.domain.usecase.trip.CreateTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SmartPackViewModel @Inject constructor(
    private val generatePackingListUseCase: GeneratePackingListUseCase,
    private val createTripUseCase: CreateTripUseCase,
    private val addChecklistItemUseCase: AddChecklistItemUseCase,
    private val authRepository: AuthRepository,
    private val stateHolder: SmartPackStateHolder
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(stateHolder.uiState ?: SmartPackUiState())
    val uiState: StateFlow<SmartPackUiState> = _uiState.asStateFlow()

    private fun updateState(reducer: (SmartPackUiState) -> SmartPackUiState) {
        val newState = reducer(_uiState.value)
        _uiState.value = newState
        stateHolder.uiState = newState
    }
    
    fun updateDestination(destination: String) {
        updateState { it.copy(destination = destination) }
    }
    
    fun updateMonth(month: String) {
        updateState { it.copy(month = month) }
    }
    
    fun updateDuration(duration: Int) {
        updateState { it.copy(tripDuration = duration) }
    }
    
    fun updateNumberOfPeople(people: Int) {
        updateState { it.copy(numberOfPeople = people) }
    }
    
    fun updateTripType(tripType: TripType) {
        updateState { it.copy(tripType = tripType) }
    }
    
    fun generatePackingList() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            
            val current = _uiState.value
            val request = SmartPackRequest(
                destination = current.destination,
                month = current.month,
                tripDuration = current.tripDuration,
                numberOfPeople = current.numberOfPeople,
                tripType = current.tripType
            )
            
            val result = generatePackingListUseCase(request)
            result.fold(
                onSuccess = { items ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            generatedItems = items,
                            showResults = true
                        )
                    }
                },
                onFailure = { error ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to generate packing list"
                        )
                    }
                }
            )
        }
    }

    /**
     * Called when the user checks/unchecks an item in the generated list.
     * This only updates local UI state. Items are pushed into the main
     * checklist feature when the user taps "Add to your List".
     */
    fun onGeneratedItemChecked(item: ChecklistItem, isChecked: Boolean) {
        // Update local UI state
        val updatedItems = _uiState.value.generatedItems.map {
            if (it.id == item.id) it.copy(isPacked = isChecked) else it
        }
        updateState { it.copy(generatedItems = updatedItems) }
    }

    /**
     * Adds all checked generated items into the main Checklist feature
     * under a trip whose name/destination matches the SmartPack destination.
     */
    fun addSelectedGeneratedItemsToChecklist() {
        viewModelScope.launch {
            val selectedItems = _uiState.value.generatedItems.filter { it.isPacked }
            if (selectedItems.isEmpty()) {
                return@launch
            }

            val destinationName = _uiState.value.destination.ifBlank { "Trip" }
            val existingTripId = _uiState.value.checklistTripId

            val tripId = if (existingTripId != null) {
                existingTripId
            } else {
                val now = Date()
                val userId = authRepository.getCurrentUser()?.id
                val createResult = createTripUseCase(
                    name = destinationName,
                    destination = destinationName,
                    startDate = now,
                    endDate = now,
                    numberOfPeople = _uiState.value.numberOfPeople,
                    tripType = _uiState.value.tripType,
                    userId = userId
                )

                val createdId = createResult.getOrNull()
                if (createdId == null) {
                    // Surface a simple error but keep SmartPack usable.
                    updateState {
                        it.copy(
                            error = createResult.exceptionOrNull()?.message
                                ?: "Failed to create checklist for this trip"
                        )
                    }
                    return@launch
                }

                updateState { it.copy(checklistTripId = createdId) }
                createdId
            }

            selectedItems.forEach { selected ->
                addChecklistItemUseCase(
                    tripId = tripId,
                    name = selected.name,
                    category = selected.category
                )
            }
        }
    }
    
    fun clearResults() {
        updateState {
            it.copy(
                showResults = false,
                generatedItems = emptyList()
            )
        }
    }
    
    fun clearError() {
        updateState { it.copy(error = null) }
    }
}

data class SmartPackUiState(
    val destination: String = "",
    val month: String = "",
    val tripDuration: Int = 1,
    val numberOfPeople: Int = 1,
    val tripType: TripType = TripType.VACATION,
    val isLoading: Boolean = false,
    val generatedItems: List<ChecklistItem> = emptyList(),
    val showResults: Boolean = false,
    val error: String? = null,
    val checklistTripId: String? = null
)
