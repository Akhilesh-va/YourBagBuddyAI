package com.example.yourbagbuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Email: standard format. */
private val EMAIL_REGEX = Regex(
    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
)

/** Phone (optional): when provided, allow +, digits, spaces, dashes, parens; at least 10 digit chars. */
private val PHONE_REGEX = Regex("^[+]?[0-9\\s\\-\\(\\)]{10,}$")

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    /** True when profile was saved; UI should navigate to Home. */
    private val _profileSaved = MutableStateFlow(false)
    val profileSaved: StateFlow<Boolean> = _profileSaved.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _uiState.value = _uiState.value.copy(
                displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: "",
                email = user?.email?.takeIf { it?.isNotBlank() == true } ?: "",
                phoneNumber = user?.phoneNumber ?: "",
                hasExistingUser = user != null
            )
        }
    }

    fun updateDisplayName(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name, error = null)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            error = null,
            passwordHasMinLength = password.length >= 6,
            passwordHasNumber = password.any { it.isDigit() },
            passwordHasSymbol = password.any { !it.isLetterOrDigit() }
        )
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = confirmPassword,
            error = null
        )
    }

    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phoneNumber, error = null)
    }

    fun saveProfile() {
        val name = _uiState.value.displayName.trim()
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val confirm = _uiState.value.confirmPassword
        val phone = _uiState.value.phoneNumber.trim()

        when {
            name.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Name is required")
                return
            }
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Email is required")
                return
            }
            !EMAIL_REGEX.matches(email) -> {
                _uiState.value = _uiState.value.copy(error = "Enter a valid email address")
                return
            }
            !_uiState.value.hasExistingUser && password.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Password is required")
                return
            }
            !_uiState.value.hasExistingUser && password.length < 6 -> {
                _uiState.value = _uiState.value.copy(error = "Password must be at least 6 characters")
                return
            }
            !_uiState.value.hasExistingUser && !password.any { it.isDigit() } -> {
                _uiState.value = _uiState.value.copy(error = "Password must include at least 1 number")
                return
            }
            !_uiState.value.hasExistingUser && !password.any { !it.isLetterOrDigit() } -> {
                _uiState.value = _uiState.value.copy(error = "Password must include at least 1 symbol")
                return
            }
            !_uiState.value.hasExistingUser && confirm != password -> {
                _uiState.value = _uiState.value.copy(error = "Passwords do not match")
                return
            }
            phone.isNotBlank() && !PHONE_REGEX.matches(phone) -> {
                _uiState.value = _uiState.value.copy(error = "Enter a valid phone number")
                return
            }
        }

        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Decide if this should create a NEW auth account, or just update the
            // profile for the currently signed-in user.
            val isNewAccount = user == null ||
                (user.email != null && !user.email.equals(email, ignoreCase = true))

            if (isNewAccount) {
                // If another user is already signed in but a different email is entered,
                // sign out and create a brand new account for the new email.
                if (user != null) {
                    authRepository.signOut()
                }

                val signUpResult = authRepository.signUpWithEmail(email, password)
                val newUser = signUpResult.getOrElse { e ->
                    val message = when {
                        e.message?.contains("email address is already in use", ignoreCase = true) == true ||
                                e.message?.contains("EMAIL_EXISTS", ignoreCase = true) == true ->
                            "An account with this email already exists."
                        e.message?.contains("WEAK_PASSWORD", ignoreCase = true) == true ->
                            "Password is too weak. Use a stronger password."
                        e.message?.contains("INVALID_EMAIL", ignoreCase = true) == true ->
                            "Enter a valid email address."
                        else -> e.message ?: "Sign up failed"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = message
                    )
                    return@launch
                }

                val profileResult = authRepository.saveUserProfile(
                    userId = newUser.id,
                    displayName = name,
                    email = email,
                    phoneNumber = phone
                )

                profileResult.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                        _profileSaved.value = true
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to save profile"
                        )
                    }
                )
            } else {
                // Same email as the signed-in user: just update profile fields.
                val profileEmail = user.email ?: email
                val profileResult = authRepository.saveUserProfile(
                    userId = user.id,
                    displayName = name,
                    email = profileEmail,
                    phoneNumber = phone.ifBlank { user.phoneNumber ?: "" }
                )

                profileResult.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                        _profileSaved.value = true
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to save profile"
                        )
                    }
                )
            }
        }
    }

    fun clearProfileSaved() {
        _profileSaved.value = false
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class SignupUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val passwordHasMinLength: Boolean = false,
    val passwordHasNumber: Boolean = false,
    val passwordHasSymbol: Boolean = false,
    val hasExistingUser: Boolean = false
)
