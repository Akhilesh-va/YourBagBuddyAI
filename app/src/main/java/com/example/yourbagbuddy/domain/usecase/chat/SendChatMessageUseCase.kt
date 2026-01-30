package com.example.yourbagbuddy.domain.usecase.chat

import com.example.yourbagbuddy.domain.model.ChatMessage
import com.example.yourbagbuddy.domain.repository.ChatRepository
import javax.inject.Inject

class SendChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {

    suspend operator fun invoke(
        message: String,
        history: List<ChatMessage>
    ): Result<String> = chatRepository.sendMessage(message, history)
}
