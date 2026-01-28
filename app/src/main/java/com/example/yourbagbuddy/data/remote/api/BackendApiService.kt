package com.example.yourbagbuddy.data.remote.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit service for YourBagBuddy backend API.
 *
 * All endpoints require a Firebase ID token in the Authorization header.
 * The backend verifies this token and uses the uid for user-scoped operations.
 */
interface BackendApiService {

    /**
     * Generate a smart packing list based on trip details.
     * Backend calls AI provider server-side, so no API keys are exposed in the app.
     */
    @POST("api/ai/packing-list")
    suspend fun generatePackingList(
        @Header("Authorization") authHeader: String,
        @Body request: PackingListRequest
    ): PackingListResponse

    /**
     * Get a travel tip from the AI.
     */
    @GET("api/ai/travel-tip")
    suspend fun getTravelTip(
        @Header("Authorization") authHeader: String
    ): TravelTipResponse
}

// --- Request DTOs ---
// Backend expects camelCase (tripDuration, numberOfPeople, tripType).

data class PackingListRequest(
    val destination: String,
    val month: String,
    val tripDuration: Int,
    val numberOfPeople: Int,
    val tripType: String
)

// --- Response DTOs ---

data class PackingListResponse(
    val items: List<PackingItemDto>?,
    val error: String? = null
)

data class PackingItemDto(
    val name: String,
    val category: String?,
    val quantity: Int?
)

data class TravelTipResponse(
    val tip: String?,
    val error: String? = null
)
