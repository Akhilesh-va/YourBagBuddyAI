package com.example.yourbagbuddy.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.yourbagbuddy.domain.model.ChatMessage
import com.example.yourbagbuddy.domain.model.ChatRole
import com.example.yourbagbuddy.presentation.components.AuthRequiredDialog
import com.example.yourbagbuddy.presentation.components.ModernTopAppBar
import com.example.yourbagbuddy.presentation.ui.theme.Primary
import com.example.yourbagbuddy.presentation.viewmodel.AuthStatusViewModel
import com.example.yourbagbuddy.presentation.viewmodel.ChatViewModel

private val BubbleShapeUser = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
private val BubbleShapeBot = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
private val InputBarShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel(),
    authStatusViewModel: AuthStatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val isLoggedIn by authStatusViewModel.isLoggedIn.collectAsState()
    var showAuthDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) showAuthDialog = false else showAuthDialog = true
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    if (showAuthDialog) {
        AuthRequiredDialog(
            onDismiss = {
                showAuthDialog = false
                onNavigateBack()
            },
            onSignUp = {
                showAuthDialog = false
                onNavigateToSignUp()
            },
            onLogin = {
                showAuthDialog = false
                onNavigateToLogin()
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 2.dp,
                tonalElevation = 0.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                ModernTopAppBar(
                    title = "Packing Assistant",
                    showBackButton = true,
                    onBackClick = onNavigateBack
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Chat list with subtle background
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            )
                        )
                    ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        WelcomeCard()
                    }
                }
                items(uiState.messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }
                if (uiState.isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            uiState.error?.let { error ->
                ErrorBanner(
                    message = error,
                    onDismiss = { viewModel.clearError() }
                )
            }

            // Modern floating input bar
            InputBar(
                value = uiState.inputText,
                onValueChange = viewModel::updateInputText,
                onSend = { viewModel.sendMessage() },
                enabled = !uiState.isLoading && uiState.inputText.isNotBlank()
            )
        }
    }
}

@Composable
private fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = null
    ) {
        Text(
            text = "Ask me anything about packing, weather, or what to bring for your trip. I use the same AI that powers your packing list.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val backgroundColor = if (isUser) Primary else MaterialTheme.colorScheme.surface
        val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

        Surface(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .shadow(4.dp, if (isUser) BubbleShapeUser else BubbleShapeBot, spotColor = Color.Black.copy(alpha = 0.08f)),
            shape = if (isUser) BubbleShapeUser else BubbleShapeBot,
            color = backgroundColor,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = BubbleShapeBot,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.shadow(4.dp, BubbleShapeBot, spotColor = Color.Black.copy(alpha = 0.06f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Thinking...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    val containerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface,
        label = "input_bar_bg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(8.dp, InputBarShape, spotColor = Color.Black.copy(alpha = 0.08f)),
        shape = InputBarShape,
        color = containerColor,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                placeholder = {
                    Text(
                        "Ask about packing...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                maxLines = 3,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = Primary
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            FilledIconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 4.dp),
                enabled = enabled,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFFFF9800),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = Color.White,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
