package com.example.yourbagbuddy.presentation.viewmodel

import com.example.yourbagbuddy.data.remote.api.BackendApiService
import com.example.yourbagbuddy.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client to fetch travel tips from the YourBagBuddy backend API.
 *
 * The backend handles all AI provider interactions server-side,
 * keeping API keys secure.
 */
@Singleton
class TravelTipService @Inject constructor(
    private val backendApiService: BackendApiService,
    private val authRepository: AuthRepository
) {

    companion object {
        private const val FALLBACK_TIP = "Pack light, pack smart! Roll your clothes to save space."
    }

    /**
     * Fetches a single short, practical travel tip from the backend.
     * Returns a fallback tip string if anything goes wrong or user is not authenticated.
     */
    suspend fun fetchTravelTip(): String = withContext(Dispatchers.IO) {
        try {
            val idToken = authRepository.getIdToken()
            if (idToken == null) {
                return@withContext FALLBACK_TIP
            }

            val authHeader = "Bearer $idToken"
            val response = backendApiService.getTravelTip(authHeader)

            response.tip?.takeIf { it.isNotBlank() } ?: FALLBACK_TIP
        } catch (_: Exception) {
            FALLBACK_TIP
        }
    }
}

