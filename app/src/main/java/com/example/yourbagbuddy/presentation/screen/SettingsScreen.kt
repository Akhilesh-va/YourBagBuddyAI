package com.example.yourbagbuddy.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.yourbagbuddy.R
import com.example.yourbagbuddy.presentation.viewmodel.SettingsViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToTravelMedicine: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    var showAuthChoiceDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }
    var feedbackRating by remember { mutableStateOf(0) }
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            com.example.yourbagbuddy.presentation.components.ModernTopAppBar(
                title = "Profile",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (showAuthChoiceDialog) {
                AlertDialog(
                    onDismissRequest = { showAuthChoiceDialog = false },
                    title = {
                        Text(
                            text = "Sign in",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Choose whether you want to log in to an existing account or create a new one.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showAuthChoiceDialog = false
                                onNavigateToLogin()
                            }
                        ) {
                            Text("Log In")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showAuthChoiceDialog = false
                                onNavigateToSignUp()
                            }
                        ) {
                            Text("Sign Up")
                        }
                    }
                )
            }

            if (showSignOutConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showSignOutConfirmDialog = false },
                    title = {
                        Text(
                            text = "Sign out?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to sign out?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showSignOutConfirmDialog = false
                                viewModel.signOut()
                            }
                        ) {
                            Text("Sign Out")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showSignOutConfirmDialog = false }
                        ) {
                            Text("Changed my mind")
                        }
                    }
                )
            }

            if (showFeedbackDialog) {
                FeedbackDialog(
                    message = feedbackMessage,
                    onMessageChange = { feedbackMessage = it },
                    rating = feedbackRating,
                    onRatingChange = { feedbackRating = it },
                    sending = uiState.feedbackSending,
                    success = uiState.feedbackSuccess,
                    error = uiState.feedbackError,
                    onDismiss = {
                        showFeedbackDialog = false
                        viewModel.clearFeedbackState()
                        feedbackMessage = ""
                        feedbackRating = 0
                    },
                    onSubmit = {
                        viewModel.submitFeedback(
                            message = feedbackMessage,
                            rating = feedbackRating,
                            name = currentUser?.displayName ?: currentUser?.email?.substringBefore('@') ?: "",
                            emailId = currentUser?.email ?: ""
                        )
                    },
                    onSuccessDismiss = {
                        showFeedbackDialog = false
                        viewModel.clearFeedbackState()
                        feedbackMessage = ""
                        feedbackRating = 0
                        android.widget.Toast.makeText(context, "Thank you! Your feedback was sent.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        
            // Account Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                if (currentUser != null) {
                    Text(
                        text = "Signed in as: ${currentUser?.email ?: currentUser?.displayName ?: "User"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { showSignOutConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            "Sign Out",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = "Guest Mode",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Sign in to sync your data across devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                    onClick = { showAuthChoiceDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            "Sign In",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        
            // Travel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Travel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Medicine suggestions by category for your trips",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onNavigateToTravelMedicine,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Travel medicine")
                    }
                }
            }

            // App Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode")
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onDarkThemeChange(it) }
                    )
                }
            }
        }

            // Contact Us
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Contact Us",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Share your feedback or report an issue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { showFeedbackDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Give Feedback")
                    }
                }
            }
        
            // About
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "YourBagBuddy v1.0",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://docs.google.com/document/d/1eYbU6c20GbjzdEyLpQhNzXduVeJLu9u7bzA8BlfnYC0/edit?usp=sharing")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Privacy Policy")
                }
                TextButton(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://docs.google.com/document/d/1FJnkDFumjrXnjUZce7mBwTNZYfWag1AI7T6K7bZPP2A/edit?usp=sharing")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Terms & Conditions")
                }
            }
        }
        }
    }
}

@Composable
private fun FeedbackDialog(
    message: String,
    onMessageChange: (String) -> Unit,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    sending: Boolean,
    success: Boolean?,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onSuccessDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Lottie: welcoming hand animation—form state loops; success plays once
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.feedback_traveller)
                )
                val lottieProgress by animateLottieCompositionAsState(
                    composition,
                    iterations = if (success == true) 1 else LottieConstants.IterateForever
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { lottieProgress },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                }
                Text(
                    text = if (success == true) "Thank you!" else "Give Feedback",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (success == true) {
                    Text(
                        text = "Your feedback was sent. We'll get back to you if needed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onSuccessDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("OK")
                    }
                } else {
                    OutlinedTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        label = { Text("Message / Issue") },
                        placeholder = { Text("Describe your feedback or issue...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        enabled = !sending
                    )
                    Text(
                        text = "Rate us",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { if (!sending) onRatingChange(star) },
                                enabled = !sending
                            ) {
                                Icon(
                                    imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "Rate $star star${if (star == 1) "" else "s"}",
                                    tint = if (star <= rating)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    if (error != null) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = onSubmit,
                            enabled = !sending && message.isNotBlank() && rating in 1..5,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (sending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Send")
                            }
                        }
                    }
                }
            }
        }
    }
}
