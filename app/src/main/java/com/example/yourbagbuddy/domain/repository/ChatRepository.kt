package com.example.yourbagbuddy.domain.repository

import com.example.yourbagbuddy.domain.model.ChatMessage

/**
 * Repository for the packing-assistant chat.
 * Uses the same backend AI as packing-list generation.
 */
interface ChatRepository {

    /**
     * Sends a user message and returns the AI reply.
     * [history] is the recent conversation for context (optional; backend may limit length).
     */
    suspend fun sendMessage(
        message: String,
        history: List<ChatMessage>
    ): Result<String>
}
