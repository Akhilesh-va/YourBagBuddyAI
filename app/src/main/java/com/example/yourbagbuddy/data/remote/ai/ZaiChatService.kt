package com.example.yourbagbuddy.data.remote.ai

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Retrofit service for the Z.AI chat completions endpoint.
 *
 * This layer is deliberately minimal and provider-specific. The rest of the
 * app should only talk to [SmartPackRepository], never directly to this API.
 */
interface ZaiChatService {

    @Headers(
        "Content-Type: application/json",
        "Accept-Language: en-US,en"
    )
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Body body: ChatCompletionRequest
    ): ChatCompletionResponse
}

// --- Request DTOs ---

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    @SerializedName("max_tokens")
    val maxTokens: Int,
    val stream: Boolean = false
)

data class ChatMessage(
    val role: String,
    val content: String
)

// --- Response DTOs ---

data class ChatCompletionResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: ChoiceMessage?
)

data class ChoiceMessage(
    val role: String?,
    val content: String?
)

