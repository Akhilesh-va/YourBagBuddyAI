package com.example.yourbagbuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.domain.model.Trip
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.domain.repository.TripRepository
import com.example.yourbagbuddy.domain.usecase.trip.CreateTripUseCase
import com.example.yourbagbuddy.domain.usecase.trip.DeleteTripUseCase
import com.example.yourbagbuddy.domain.usecase.trip.GetAllTripsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TripsViewModel @Inject constructor(
    private val getAllTripsUseCase: GetAllTripsUseCase,
    private val createTripUseCase: CreateTripUseCase,
    private val deleteTripUseCase: DeleteTripUseCase,
    private val authRepository: AuthRepository,
    private val tripRepository: TripRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TripsUiState())
    val uiState: StateFlow<TripsUiState> = _uiState.asStateFlow()
    
    init {
        loadTrips()
    }
    
    private fun loadTrips() {
        viewModelScope.launch {
            getAllTripsUseCase().collect { trips ->
                _uiState.value = _uiState.value.copy(
                    trips = trips,
                    isLoading = false
                )
            }
        }
    }
    
    fun createTrip(
        name: String,
        destination: String,
        startDate: Date,
        endDate: Date,
        numberOfPeople: Int,
        tripType: com.example.yourbagbuddy.domain.model.TripType
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val user = authRepository.getCurrentUser()
            val result = createTripUseCase(
                name = name,
                destination = destination,
                startDate = startDate,
                endDate = endDate,
                numberOfPeople = numberOfPeople,
                tripType = tripType,
                userId = user?.id
            )
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    // Sync if logged in
                    user?.id?.let { tripRepository.syncTrips(it) }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create trip"
                    )
                }
            )
        }
    }
    
    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = deleteTripUseCase(tripId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    // Sync if logged in
                    authRepository.getCurrentUser()?.id?.let { 
                        tripRepository.syncTrips(it) 
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete trip"
                    )
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class TripsUiState(
    val trips: List<Trip> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
