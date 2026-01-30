package com.example.yourbagbuddy.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.data.remote.api.FeedbackApiService
import com.example.yourbagbuddy.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val feedbackApiService: FeedbackApiService,
    @Named("FeedbackSheetUrl") private val feedbackSheetUrl: String
) : ViewModel() {

    companion object {
        private const val TAG = "FeedbackSubmit"
    }

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
            val url = feedbackSheetUrl.trim().removeSuffix("/")
            val msg = message.orEmpty()
            val nameVal = name.orEmpty()
            val emailVal = emailId.orEmpty()
            val contactVal = ""

            Log.d(TAG, "[START] submitFeedback")
            Log.d(TAG, "  config URL: $url")
            Log.d(TAG, "  payload: message='$msg' name='$nameVal' emailId='$emailVal' contactNo='$contactVal' rating=$rating")

            suspend fun doSubmit(targetUrl: String) {
                feedbackApiService.submitFeedback(
                    targetUrl,
                    message = msg,
                    name = nameVal,
                    emailId = emailVal,
                    contactNo = contactVal,
                    rating = rating
                )
            }

            fun errorMessage(e: Exception): String = when {
                e is HttpException && e.code() == 405 -> "Use the deployment URL from Deploy > Manage deployments (not Test deployments)."
                e is HttpException && e.code() == 302 -> "Redirect received. Use the deployment URL from Deploy > Manage deployments."
                else -> e.message ?: "Failed to send feedback"
            }

            try {
                Log.d(TAG, "[1] POST to config URL...")
                doSubmit(url)
                Log.d(TAG, "[1] POST success (200)")
                _uiState.value = _uiState.value.copy(
                    feedbackSending = false,
                    feedbackSuccess = true,
                    feedbackError = null
                )
            } catch (e: Exception) {
                val code = (e as? HttpException)?.code()
                val redirectUrl = (e as? HttpException)?.response()?.raw()?.header("Location")
                Log.d(TAG, "[1] POST failed: code=$code message=${e.message}")
                if (code == 302) {
                    Log.d(TAG, "  302 Location: ${redirectUrl?.take(80)}...")
                }

                if (e is HttpException && code == 302 && !redirectUrl.isNullOrBlank()) {
                    try {
                        val sep = if (redirectUrl.contains("?")) "&" else "?"
                        val redirectWithParams = redirectUrl + sep +
                            "message=${URLEncoder.encode(msg, StandardCharsets.UTF_8.name())}" +
                            "&name=${URLEncoder.encode(nameVal, StandardCharsets.UTF_8.name())}" +
                            "&emailId=${URLEncoder.encode(emailVal, StandardCharsets.UTF_8.name())}" +
                            "&contactNo=${URLEncoder.encode(contactVal, StandardCharsets.UTF_8.name())}" +
                            "&rating=$rating"
                        Log.d(TAG, "[2] GET to redirect URL with query params:")
                        Log.d(TAG, "  message=$msg name=$nameVal emailId=$emailVal contactNo=$contactVal rating=$rating")
                        Log.d(TAG, "  full GET URL length=${redirectWithParams.length} (see OkHttp log for full URL)")
                        feedbackApiService.submitFeedbackGet(redirectWithParams)
                        Log.d(TAG, "[2] GET success")
                        _uiState.value = _uiState.value.copy(
                            feedbackSending = false,
                            feedbackSuccess = true,
                            feedbackError = null
                        )
                    } catch (e2: Exception) {
                        val code2 = (e2 as? HttpException)?.code()
                        Log.d(TAG, "[2] GET failed: code=$code2 message=${e2.message}")
                        e2.printStackTrace()
                        _uiState.value = _uiState.value.copy(
                            feedbackSending = false,
                            feedbackSuccess = false,
                            feedbackError = errorMessage(e2)
                        )
                    }
                } else {
                    try {
                        Log.d(TAG, "[retry] delay 1500ms then POST again...")
                        delay(1500)
                        doSubmit(url)
                        Log.d(TAG, "[retry] POST success")
                        _uiState.value = _uiState.value.copy(
                            feedbackSending = false,
                            feedbackSuccess = true,
                            feedbackError = null
                        )
                    } catch (e2: Exception) {
                        Log.d(TAG, "[retry] POST failed: ${e2.message}")
                        _uiState.value = _uiState.value.copy(
                            feedbackSending = false,
                            feedbackSuccess = false,
                            feedbackError = errorMessage(e2)
                        )
                    }
                }
            }
            Log.d(TAG, "[END] submitFeedback result: success=${_uiState.value.feedbackSuccess} error=${_uiState.value.feedbackError}")
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
