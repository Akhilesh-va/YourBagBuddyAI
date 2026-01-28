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

/** Email validation used for login. Kept local to avoid cross-file private references. */
private val EMAIL_REGEX = Regex(
    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Set when email sign-in succeeds; UI should navigate (Home or CompleteProfile). */
    private val _signedInUser = MutableStateFlow<User?>(null)
    val signedInUser: StateFlow<User?> = _signedInUser.asStateFlow()

    init {
        tryCompleteEmailLinkSignIn()
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun signInWithEmail() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter your email")
            return
        }
        if (!EMAIL_REGEX.matches(email)) {
            _uiState.value = _uiState.value.copy(error = "Enter a valid email address")
            return
        }
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter your password")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithEmail(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    _signedInUser.value = user
                },
                onFailure = { e ->
                    val message = when {
                        e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ->
                            "Invalid email or password."
                        e.message?.contains("invalid", ignoreCase = true) == true ->
                            "Invalid email or password."
                        else -> e.message ?: "Sign in failed"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = message
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

    /** Try to complete sign-in if we have a stored email link (e.g. user opened the app from the email). */
    fun tryCompleteEmailLinkSignIn() {
        val link = authRepository.getAndClearPendingEmailLink() ?: return
        val email = authRepository.getPendingEmailForLink() ?: run {
            _uiState.value = _uiState.value.copy(
                error = "Open the sign-in link on the same device where you requested it."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithEmailLink(email, link)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    _signedInUser.value = user
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Sign-in link expired or invalid. Request a new link."
                    )
                }
            )
        }
    }

    fun sendEmailLink() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter your email")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.sendSignInLinkToEmail(email)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        emailLinkSent = true,
                        emailLinkSentTo = email
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to send sign-in link"
                    )
                }
            )
        }
    }

    fun clearEmailLinkSent() {
        _uiState.value = _uiState.value.copy(emailLinkSent = false, emailLinkSentTo = null)
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
                    val message = when {
                        e.message?.contains("No credentials", ignoreCase = true) == true ->
                            "Add a Google account in device Settings, or try signing in again."
                        e.message?.contains("cancel", ignoreCase = true) == true ->
                            "Sign-in was cancelled."
                        else -> e.message ?: "Google sign in failed"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = message
                    )
                }
            )
        }
    }
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val emailLinkSent: Boolean = false,
    val emailLinkSentTo: String? = null
)
