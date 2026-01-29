package com.example.yourbagbuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.data.remote.api.FeedbackApiService
import com.example.yourbagbuddy.data.remote.api.FeedbackRequestBody
import com.example.yourbagbuddy.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val feedbackApiService: FeedbackApiService,
    @Named("FeedbackSheetUrl") private val feedbackSheetUrl: String
) : ViewModel() {

    val currentUser = authRepository.currentUser

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = authRepository.signOut()
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to sign out"
                    )
                }
            )
        }
    }

    fun submitFeedback(message: String, rating: Int, name: String, emailId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                feedbackSending = true,
                feedbackSuccess = null,
                feedbackError = null
            )
            try {
                feedbackApiService.submitFeedback(
                    feedbackSheetUrl,
                    FeedbackRequestBody(
                        Message = message,
                        Name = name,
                        emailId = emailId,
                        contactNo = "",
                        Rating = rating
                    )
                )
                _uiState.value = _uiState.value.copy(
                    feedbackSending = false,
                    feedbackSuccess = true,
                    feedbackError = null
                )
            } catch (e: Exception) {
                val message = when {
                    e is HttpException && e.code() == 405 -> "Use the deployment URL from Deploy > Manage deployments (not Test deployments)."
                    e is HttpException && e.code() == 302 -> "Redirect received. Use the deployment URL from Deploy > Manage deployments."
                    else -> e.message ?: "Failed to send feedback"
                }
                _uiState.value = _uiState.value.copy(
                    feedbackSending = false,
                    feedbackSuccess = false,
                    feedbackError = message
                )
            }
        }
    }

    fun clearFeedbackState() {
        _uiState.value = _uiState.value.copy(
            feedbackSuccess = null,
            feedbackError = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val feedbackSending: Boolean = false,
    val feedbackSuccess: Boolean? = null,
    val feedbackError: String? = null
)
