package com.example.yourbagbuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.domain.model.Trip
import com.example.yourbagbuddy.domain.model.User
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.domain.usecase.trip.GetAllTripsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllTripsUseCase: GetAllTripsUseCase,
    private val authRepository: AuthRepository,
    private val travelTipService: TravelTipService
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
        refreshAiTip()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            getAllTripsUseCase().collect { trips ->
                val upcomingTrip = getUpcomingTrip(trips)
                val userTrips = user?.let { u ->
                    trips.filter { it.userId == u.id }
                } ?: trips

                _uiState.value = _uiState.value.copy(
                    upcomingTrip = upcomingTrip,
                    greeting = getGreeting(user),
                    tipOfTheDay = _uiState.value.tipOfTheDay.takeIf { it.isNotBlank() }
                        ?: TravelTipsProvider.getTipForToday(),
                    userName = user?.displayName,
                    trips = userTrips
                )
            }
        }
    }

    /**
     * Fetch an AI-generated travel tip from the backend and update UI state.
     * Falls back to the static tip provider if anything goes wrong.
     */
    fun refreshAiTip() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAiTipLoading = true,
                aiTipError = null
            )

            val fallbackTip = TravelTipsProvider.getTipForToday()

            try {
                val aiTip = travelTipService.fetchTravelTip()
                _uiState.value = _uiState.value.copy(
                    isAiTipLoading = false,
                    aiTip = aiTip,
                    tipOfTheDay = aiTip.ifBlank { fallbackTip }
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAiTipLoading = false,
                    aiTip = null,
                    aiTipError = "Unable to load AI tip. Showing a regular tip instead.",
                    tipOfTheDay = fallbackTip
                )
            }
        }
    }
    
    private fun getUpcomingTrip(trips: List<Trip>): Trip? {
        val now = Calendar.getInstance().time
        return trips
            .filter { it.startDate.after(now) }
            .minByOrNull { it.startDate.time }
    }
    
    private fun getGreeting(user: User?): String {
        return when {
            user == null -> "Hi Traveller"
            !user.displayName.isNullOrBlank() -> "Hi ${user.displayName}"
            else -> "Hi Traveller"
        }
    }
}

data class HomeUiState(
    val upcomingTrip: Trip? = null,
    val greeting: String = "Hello",
    val tipOfTheDay: String = "Pack light, pack smart! Roll your clothes to save space.",
    val aiTip: String? = null,
    val isAiTipLoading: Boolean = false,
    val aiTipError: String? = null,
    val userName: String? = null,
    val trips: List<Trip> = emptyList()
)
