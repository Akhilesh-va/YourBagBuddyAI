package com.example.yourbagbuddy.data.repository

import android.util.Log
import com.example.yourbagbuddy.data.remote.api.BackendApiService
import com.example.yourbagbuddy.data.remote.api.PackingListRequest
import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.model.ItemCategory
import com.example.yourbagbuddy.domain.model.SmartPackRequest
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.domain.repository.SmartPackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

private const val TAG = "SmartPackRepository"

/**
 * Production-ready SmartPackRepository implementation that calls the
 * YourBagBuddy backend API to generate AI-powered packing lists.
 *
 * The backend handles all AI provider interactions server-side, keeping
 * API keys secure. Auth is via Firebase ID token sent in the Authorization header.
 *
 * The rest of the app talks only in terms of [SmartPackRequest] and
 * [ChecklistItem]; all backend/network details are contained in the data layer.
 */
class SmartPackRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService,
    private val authRepository: AuthRepository
) : SmartPackRepository {

    override suspend fun generatePackingList(request: SmartPackRequest): Result<List<ChecklistItem>> {
        return withContext(Dispatchers.IO) {
            try {
                // Get Firebase ID token for authentication
                val idToken = authRepository.getIdToken()
                    ?: return@withContext Result.failure(
                        SmartPackError.AuthRequired("Please sign in to generate a packing list.")
                    )

                // Log the token for debugging (remove in production!)
                Log.d(TAG, "Firebase ID Token: $idToken")
                Log.d(TAG, "Token length: ${idToken.length}")

                val authHeader = "Bearer $idToken"

                // Map domain model to API request
                val apiRequest = PackingListRequest(
                    destination = request.destination,
                    month = request.month,
                    tripDuration = request.tripDuration,
                    numberOfPeople = request.numberOfPeople,
                    tripType = request.tripType.name.lowercase()
                )

                Log.d(TAG, "Sending request: $apiRequest")

                val response = backendApiService.generatePackingList(authHeader, apiRequest)

                Log.d(TAG, "Response received: items=${response.items?.size}, error=${response.error}")

                // Check for error in response
                if (response.error != null) {
                    return@withContext Result.failure(
                        SmartPackError.AiFailure(response.error)
                    )
                }

                // Map API response to domain model
                val items = response.items?.map { dto ->
                    ChecklistItem(
                        id = java.util.UUID.randomUUID().toString(),
                        tripId = "", // Will be set when saved to a trip
                        name = dto.name,
                        category = mapCategory(dto.category),
                        isPacked = false
                    )
                } ?: emptyList()

                if (items.isEmpty()) {
                    return@withContext Result.failure(
                        SmartPackError.AiFailure("No items were generated. Please try again.")
                    )
                }

                Result.success(items)

            } catch (e: UnknownHostException) {
                Result.failure(
                    SmartPackError.NetworkUnavailable(
                        "No internet connection. You can still use your manual checklist.",
                        e
                    )
                )
            } catch (e: SocketTimeoutException) {
                Result.failure(
                    SmartPackError.Timeout(
                        "The AI service took too long to respond. Please try again.",
                        e
                    )
                )
            } catch (e: HttpException) {
                val message = when (e.code()) {
                    401 -> "Your session has expired. Please sign in again."
                    429 -> "The AI service is receiving too many requests. Please wait a moment and try again."
                    else -> "The AI service is currently unavailable. Please try again."
                }
                Result.failure(SmartPackError.AiFailure(message, e))
            } catch (e: IOException) {
                Result.failure(
                    SmartPackError.NetworkError(
                        "A network error occurred while contacting the AI service.",
                        e
                    )
                )
            } catch (e: Exception) {
                // Catch-all to ensure the UI never crashes because of AI.
                Result.failure(
                    SmartPackError.AiFailure(
                        message = "Something went wrong while generating your packing list.",
                        cause = e
                    )
                )
            }
        }
    }

    /**
     * Maps a category string from the API to the domain ItemCategory enum.
     */
    private fun mapCategory(category: String?): ItemCategory {
        return when (category?.uppercase()) {
            "CLOTHES", "CLOTHING" -> ItemCategory.CLOTHES
            "ESSENTIALS", "ESSENTIAL" -> ItemCategory.ESSENTIALS
            "DOCUMENTS", "DOCUMENT" -> ItemCategory.DOCUMENTS
            else -> ItemCategory.OTHER
        }
    }
}

/**
 * Typed error hierarchy for SmartPack/AI failures. This is surfaced to callers
 * as an [Exception] so it still fits into the standard Kotlin [Result] type,
 * but allows the UI and future backend adapters to distinguish error classes
 * without leaking provider-specific details.
 */
sealed class SmartPackError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    class NetworkUnavailable(
        userFacingMessage: String,
        cause: Throwable? = null
    ) : SmartPackError(userFacingMessage, cause)

    class Timeout(
        userFacingMessage: String,
        cause: Throwable? = null
    ) : SmartPackError(userFacingMessage, cause)

    class NetworkError(
        userFacingMessage: String,
        cause: Throwable? = null
    ) : SmartPackError(userFacingMessage, cause)

    class InvalidAiResponse(
        val userFacingMessage: String,
        cause: Throwable? = null
    ) : SmartPackError(userFacingMessage, cause)

    class AiFailure(
        message: String,
        cause: Throwable? = null
    ) : SmartPackError(message, cause)

    class AuthRequired(
        message: String,
        cause: Throwable? = null
    ) : SmartPackError(message, cause)
}

