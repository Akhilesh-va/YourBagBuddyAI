package com.example.yourbagbuddy.presentation.viewmodel

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartPackStateHolder @Inject constructor() {
    var uiState: SmartPackUiState? = null
}

