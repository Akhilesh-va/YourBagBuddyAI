package com.example.yourbagbuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.domain.model.ChatMessage
import com.example.yourbagbuddy.domain.model.ChatRole
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.domain.usecase.chat.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            val name = user?.displayName?.takeIf { it.isNotBlank() }
                ?: user?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
                ?: "there"
            val greeting = "Hi $name, I am YourBagBuddy. How may I help you?"
            _uiState.value = _uiState.value.copy(
                messages = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = ChatRole.Assistant,
                        content = greeting
                    )
                )
            )
        }
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, error = null)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isLoading) return

        viewModelScope.launch {
            val previousMessages = _uiState.value.messages
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = ChatRole.User,
                content = text
            )
            _uiState.value = _uiState.value.copy(
                inputText = "",
                messages = previousMessages + userMessage,
                isLoading = true,
                error = null
            )

            val result = sendChatMessageUseCase(
                message = text,
                history = previousMessages
            )

            result.fold(
                onSuccess = { reply ->
                    val assistantMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = ChatRole.Assistant,
                        content = reply
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Something went wrong"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
