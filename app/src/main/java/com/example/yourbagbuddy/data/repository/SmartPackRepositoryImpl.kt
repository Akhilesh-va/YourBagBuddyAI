package com.example.yourbagbuddy.data.repository

import com.example.yourbagbuddy.data.remote.ai.ChatCompletionRequest
import com.example.yourbagbuddy.data.remote.ai.ChatMessage
import com.example.yourbagbuddy.data.remote.ai.InvalidAiResponseException
import com.example.yourbagbuddy.data.remote.ai.PackingChecklistPromptBuilder
import com.example.yourbagbuddy.data.remote.ai.PackingChecklistResponseParser
import com.example.yourbagbuddy.data.remote.ai.ZaiChatService
import com.example.yourbagbuddy.domain.model.ChecklistItem
import com.example.yourbagbuddy.domain.model.SmartPackRequest
import com.example.yourbagbuddy.domain.repository.SmartPackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * Production-ready SmartPackRepository implementation that delegates to
 * an AI provider (currently OpenRouter's OpenAI-compatible API) to generate
 * structured packing lists.
 *
 * The rest of the app talks only in terms of [SmartPackRequest] and
 * [ChecklistItem]; all provider-specific details (prompts, JSON formats,
 * HTTP errors) are fully contained in the data layer.
 */
class SmartPackRepositoryImpl @Inject constructor(
    private val zaiChatService: ZaiChatService,
    private val promptBuilder: PackingChecklistPromptBuilder,
    private val responseParser: PackingChecklistResponseParser
) : SmartPackRepository {

    // Tuned for predictable, low-cost, low-variance responses.
    // Use an OpenRouter model identifier.
    private val modelName = "openai/gpt-4.1-mini"
    private val temperature = 0.2
    private val maxTokens = 512

    override suspend fun generatePackingList(request: SmartPackRequest): Result<List<ChecklistItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = promptBuilder.build(request)

                val apiRequest = ChatCompletionRequest(
                    model = modelName,
                    messages = listOf(
                        ChatMessage(
                            role = "system",
                            content = prompt.system
                        ),
                        ChatMessage(
                            role = "user",
                            content = prompt.user
                        )
                    ),
                    temperature = temperature,
                    maxTokens = maxTokens,
                    stream = false
                )

                val response = zaiChatService.createChatCompletion(apiRequest)

                val content = response.choices
                    ?.firstOrNull()
                    ?.message
                    ?.content
                    ?.trim()
                    ?: return@withContext Result.failure(
                        SmartPackError.AiFailure("AI did not return any content.")
                    )

                // Validate and normalise content into domain-safe checklist items.
                val parsedResult = responseParser.parseToChecklistItems(content)

                parsedResult.fold(
                    onSuccess = { items -> Result.success(items) },
                    onFailure = { error ->
                        if (error is InvalidAiResponseException) {
                            Result.failure(
                                SmartPackError.InvalidAiResponse(
                                    userFacingMessage = "We couldn't understand the AI response. Please try again.",
                                    cause = error
                                )
                            )
                        } else {
                            Result.failure(
                                SmartPackError.AiFailure(
                                    message = "Unexpected AI response error.",
                                    cause = error
                                )
                            )
                        }
                    }
                )
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
                val message = if (e.code() == 429) {
                    "The AI service is receiving too many requests. Please wait a moment and try again."
                } else {
                    "The AI service is currently unavailable. Please try again."
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
}

