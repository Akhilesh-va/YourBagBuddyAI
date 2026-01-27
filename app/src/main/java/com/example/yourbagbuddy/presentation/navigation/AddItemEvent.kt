package com.example.yourbagbuddy.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AddItemEvent {
    var triggerAddDialog by mutableStateOf(false)
        private set
    
    fun trigger() {
        triggerAddDialog = true
    }
    
    fun consume() {
        triggerAddDialog = false
    }
}
