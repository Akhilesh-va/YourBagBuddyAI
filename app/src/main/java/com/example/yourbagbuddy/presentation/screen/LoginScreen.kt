package com.example.yourbagbuddy.presentation.screen

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.yourbagbuddy.R
import com.example.yourbagbuddy.domain.model.User
import com.example.yourbagbuddy.presentation.ui.theme.Primary
import com.example.yourbagbuddy.presentation.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {},
    onSignedIn: (User) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val state by viewModel.uiState.collectAsState()
    val signedInUser by viewModel.signedInUser.collectAsState()

    LaunchedEffect(signedInUser) {
        signedInUser?.let { user ->
            onSignedIn(user)
            viewModel.clearSignedInUser()
        }
    }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.login_traveller)
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    val infiniteTransition = rememberInfiniteTransition(label = "loginShimmer")
    val subtleScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            )
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onNavigateToHome) {
                Text(
                    "Skip",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LottieAnimation(
            composition = composition,
            progress = { lottieProgress },
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = subtleScale
                    scaleY = subtleScale
                }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Sign in with your email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text("Email") },
                    placeholder = { Text("you@example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = Primary,
                        cursorColor = Primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = Primary,
                        cursorColor = Primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                state.error?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = { viewModel.signInWithEmail() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            "Log in",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
                Text(
                    text = "or",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (state.emailLinkSent) {
                    val sentTo = state.emailLinkSentTo ?: ""
                    Text(
                        text = "Check $sentTo for the sign-in link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { viewModel.clearEmailLinkSent() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use password instead", color = Primary, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable(enabled = !state.isLoading) { viewModel.sendEmailLink() },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Sign in with email link",
                                    tint = Primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable(enabled = !state.isLoading) { activity?.let { viewModel.signInWithGoogle(it) } },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_google),
                                    contentDescription = "Sign in with Google",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToSignUp) {
            Text(
                "Don't have an account? ",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Sign up",
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
