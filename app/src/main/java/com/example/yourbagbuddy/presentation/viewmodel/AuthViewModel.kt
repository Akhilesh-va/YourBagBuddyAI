package com.example.yourbagbuddy.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.domain.model.User
import com.example.yourbagbuddy.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Set when phone verify succeeds; UI should navigate (Home or CompleteProfile). */
    private val _signedInUser = MutableStateFlow<User?>(null)
    val signedInUser: StateFlow<User?> = _signedInUser.asStateFlow()

    fun updatePhoneNumber(phone: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phone, error = null)
    }

    fun updateOtpCode(code: String) {
        _uiState.value = _uiState.value.copy(otpCode = code, error = null)
    }

    fun sendOtp(activity: Activity) {
        val phone = _uiState.value.phoneNumber.trim()
        if (phone.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter phone number")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.sendPhoneOtp(phone, activity)
            result.fold(
                onSuccess = { verificationId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        step = AuthStep.OTP_ENTRY,
                        verificationId = verificationId,
                        error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to send OTP"
                    )
                }
            )
        }
    }

    fun verifyOtp() {
        val verificationId = _uiState.value.verificationId
        val code = _uiState.value.otpCode.trim()
        if (verificationId == null || code.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter the code sent to your phone")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.verifyPhoneOtp(verificationId, code)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    _signedInUser.value = user
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Invalid code"
                    )
                }
            )
        }
    }

    fun clearSignedInUser() {
        _signedInUser.value = null
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun backToPhoneEntry() {
        _uiState.value = _uiState.value.copy(
            step = AuthStep.PHONE_ENTRY,
            verificationId = null,
            otpCode = "",
            error = null
        )
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithGoogle(activity)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    _signedInUser.value = user
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Google sign in failed"
                    )
                }
            )
        }
    }
}

enum class AuthStep {
    PHONE_ENTRY,
    OTP_ENTRY
}

data class AuthUiState(
    val phoneNumber: String = "",
    val verificationId: String? = null,
    val otpCode: String = "",
    val step: AuthStep = AuthStep.PHONE_ENTRY,
    val isLoading: Boolean = false,
    val error: String? = null
)
