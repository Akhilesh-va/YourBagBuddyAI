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
                phoneNumber = user?.phoneNumber ?: ""
            )
        }
    }

    fun updateDisplayName(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name, error = null)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun saveProfile() {
        val name = _uiState.value.displayName.trim()
        val phone = _uiState.value.phoneNumber
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter your name")
            return
        }
        if (phone.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Phone number is missing")
            return
        }
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: run {
                _uiState.value = _uiState.value.copy(error = "Not signed in")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.saveUserProfile(
                userId = user.id,
                displayName = name,
                email = _uiState.value.email.trim().takeIf { it.isNotBlank() },
                phoneNumber = phone
            )
            result.fold(
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
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
