package com.example.yourbagbuddy.domain.model

/**
 * A single message in the packing-assistant chat.
 * Uses the same AI as the packing list generator.
 */
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ChatRole {
    User,
    Assistant
}
