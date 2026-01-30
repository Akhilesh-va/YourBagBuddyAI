package com.example.yourbagbuddy.data.repository

import android.util.Log
import com.example.yourbagbuddy.data.remote.api.BackendApiService
import com.example.yourbagbuddy.data.remote.api.ChatMessageDto
import com.example.yourbagbuddy.data.remote.api.ChatRequest
import com.example.yourbagbuddy.domain.model.ChatMessage
import com.example.yourbagbuddy.domain.model.ChatRole
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "ChatRepository"

/**
 * Chat repository that calls the YourBagBuddy backend API.
 * Uses the same AI as packing-list generation; backend holds keys server-side.
 */
class ChatRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService,
    private val authRepository: AuthRepository
) : ChatRepository {

    override suspend fun sendMessage(
        message: String,
        history: List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val idToken = authRepository.getIdToken()
                ?: return@withContext Result.failure(
                    ChatError.AuthRequired("Please sign in to chat with the packing assistant.")
                )

            val authHeader = "Bearer $idToken"
            val historyDto = history.takeLast(MAX_HISTORY_MESSAGES).map { msg ->
                ChatMessageDto(
                    role = msg.role.name.lowercase(),
                    content = msg.content
                )
            }
            val request = ChatRequest(
                message = message,
                history = historyDto
            )

            val response = backendApiService.sendChatMessage(authHeader, request)

            if (response.error != null) {
                return@withContext Result.failure(ChatError.AiFailure(response.error))
            }
            response.reply?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(ChatError.AiFailure("No reply from assistant."))
            Result.success(response.reply)
        } catch (e: Exception) {
            Log.e(TAG, "Chat request failed", e)
            Result.failure(ChatError.Network(e.message ?: "Network error"))
        }
    }

    sealed class ChatError(message: String) : Exception(message) {
        class AuthRequired(message: String) : ChatError(message)
        class AiFailure(message: String) : ChatError(message)
        class Network(message: String) : ChatError(message)
    }

    companion object {
        private const val MAX_HISTORY_MESSAGES = 20
    }
}
