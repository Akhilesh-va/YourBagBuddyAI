package com.example.yourbagbuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourbagbuddy.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lightweight ViewModel that exposes whether the user is currently authenticated.
 * Backed by Firebase auth via [AuthRepository.currentUser].
 *
 * This is intentionally minimal so it can be safely used from any screen
 * (e.g. to gate premium / AI features) without pulling in the full login flows.
 */
@HiltViewModel
class AuthStatusViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                _isLoggedIn.value = user != null
            }
        }
    }
}

