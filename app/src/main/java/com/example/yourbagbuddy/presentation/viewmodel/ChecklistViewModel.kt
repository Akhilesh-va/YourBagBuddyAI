package com.example.yourbagbuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.model.ItemCategory
import com.example.yourbagbuddy.domain.model.Trip
import com.example.yourbagbuddy.domain.model.TripType
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.domain.repository.ChecklistRepository
import com.example.yourbagbuddy.domain.usecase.checklist.AddChecklistItemUseCase
import com.example.yourbagbuddy.domain.usecase.checklist.DeleteChecklistItemUseCase
import com.example.yourbagbuddy.domain.usecase.checklist.GetChecklistItemsUseCase
import com.example.yourbagbuddy.domain.usecase.checklist.ToggleItemPackedUseCase
import com.example.yourbagbuddy.domain.usecase.trip.CreateTripUseCase
import com.example.yourbagbuddy.domain.usecase.trip.DeleteTripUseCase
import com.example.yourbagbuddy.domain.usecase.trip.GetAllTripsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ChecklistViewModel @Inject constructor(
    private val getAllTripsUseCase: GetAllTripsUseCase,
    private val getChecklistItemsUseCase: GetChecklistItemsUseCase,
    private val addChecklistItemUseCase: AddChecklistItemUseCase,
    private val toggleItemPackedUseCase: ToggleItemPackedUseCase,
    private val deleteChecklistItemUseCase: DeleteChecklistItemUseCase,
    private val createTripUseCase: CreateTripUseCase,
    private val deleteTripUseCase: DeleteTripUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()
    
    private val selectedTripId = MutableStateFlow<String?>(null)
    
    init {
        loadChecklist()
    }
    
    private fun loadChecklist() {
        viewModelScope.launch {
            getAllTripsUseCase().collect { trips ->
                val currentSelected = selectedTripId.value
                val nextSelected = if (trips.any { it.id == currentSelected }) {
                    currentSelected
                } else {
                    trips.firstOrNull()?.id
                }

                selectedTripId.value = nextSelected
                _uiState.value = _uiState.value.copy(
                    trips = trips,
                    hasTrip = trips.isNotEmpty()
                )
            }
        }

        viewModelScope.launch {
            combine(selectedTripId, getAllTripsUseCase()) { tripId, trips ->
                val selectedTrip = trips.firstOrNull { it.id == tripId }
                _uiState.value = _uiState.value.copy(
                    selectedTripId = tripId,
                    checklistName = selectedTrip?.name ?: ""
                )
                tripId
            }.flatMapLatest { tripId ->
                if (tripId != null) {
                    getChecklistItemsUseCase(tripId)
                } else {
                    flowOf(emptyList())
                }
            }.collect { items ->
                updateUiState(items)
            }
        }
    }
    
    private fun updateUiState(items: List<ChecklistItem>) {
        val currentTripId = selectedTripId.value

        // Only auto-delete the trip when the user had items and deleted the last one.
        // Do NOT delete when totalCount is already 0 (e.g. newly created empty checklist).
        if (currentTripId != null && items.isEmpty() && _uiState.value.totalCount > 0) {
            viewModelScope.launch {
                deleteTripUseCase(currentTripId)
                selectedTripId.value = null
            }

            _uiState.value = _uiState.value.copy(
                items = emptyList(),
                completedCount = 0,
                totalCount = 0,
                isLoading = false,
                trips = _uiState.value.trips.filterNot { it.id == currentTripId },
                selectedTripId = null,
                hasTrip = _uiState.value.trips.any { it.id != currentTripId }
            )
            return
        }

        val completedCount = items.count { it.isPacked }
        val totalCount = items.size
        _uiState.value = _uiState.value.copy(
            items = items.sortedBy { it.isPacked }, // Show incomplete items first
            completedCount = completedCount,
            totalCount = totalCount,
            isLoading = false
        )
    }
    
    fun addItem(name: String, category: ItemCategory = ItemCategory.OTHER) {
        viewModelScope.launch {
            val tripId = selectedTripId.value
            if (tripId == null) {
                return@launch
            }
            
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

    fun createChecklistAndAddItem(
        checklistName: String,
        itemName: String,
        category: ItemCategory = ItemCategory.OTHER
    ) {
        viewModelScope.launch {
            val trimmedChecklistName = checklistName.trim()
            val trimmedItemName = itemName.trim()
            if (trimmedChecklistName.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    error = "Checklist name cannot be empty"
                )
                return@launch
            }
            if (trimmedItemName.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    error = "Item name cannot be empty"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val now = Date()
            val userId = authRepository.getCurrentUser()?.id
            val createResult = createTripUseCase(
                name = trimmedChecklistName,
                destination = trimmedChecklistName,
                startDate = now,
                endDate = now,
                numberOfPeople = 1,
                tripType = TripType.VACATION,
                userId = userId
            )

            createResult.fold(
                onSuccess = { tripId ->
                    selectedTripId.value = tripId
                    val addResult = addChecklistItemUseCase(tripId, trimmedItemName, category)
                    addResult.fold(
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
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create checklist"
                    )
                }
            )
        }
    }
    
    fun toggleItem(itemId: String, isPacked: Boolean) {
        viewModelScope.launch {
            toggleItemPackedUseCase(itemId, isPacked)
        }
    }
    
    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val tripId = selectedTripId.value
            val shouldDeleteTrip = _uiState.value.totalCount <= 1 && tripId != null

            val result = deleteChecklistItemUseCase(itemId)
            result.fold(
                onSuccess = {
                    if (shouldDeleteTrip) {
                        deleteTripUseCase(tripId!!)
                        selectedTripId.value = null
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete item"
                    )
                }
            )
        }
    }

    fun selectTrip(tripId: String) {
        selectedTripId.value = tripId
    }

    fun createChecklist(name: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    error = "Checklist name cannot be empty"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val now = Date()
            val userId = authRepository.getCurrentUser()?.id
            val result = createTripUseCase(
                name = trimmed,
                destination = trimmed,
                startDate = now,
                endDate = now,
                numberOfPeople = 1,
                tripType = TripType.VACATION,
                userId = userId
            )

            result.fold(
                onSuccess = { tripId ->
                    selectedTripId.value = tripId
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create checklist"
                    )
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ChecklistUiState(
    val items: List<ChecklistItem> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val hasTrip: Boolean = false,
    val checklistName: String = "",
    val trips: List<Trip> = emptyList(),
    val selectedTripId: String? = null,
    val error: String? = null
)
