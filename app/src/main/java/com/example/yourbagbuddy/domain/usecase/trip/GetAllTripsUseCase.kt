package com.example.yourbagbuddy.domain.usecase.trip

import com.example.yourbagbuddy.domain.model.Trip
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class GetAllTripsUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<List<Trip>> {
        return authRepository.currentUser.flatMapLatest { user ->
            tripRepository.getAllTrips(user?.id)
        }
    }
}
