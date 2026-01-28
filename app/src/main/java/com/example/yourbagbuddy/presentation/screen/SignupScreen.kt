package com.example.yourbagbuddy.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.yourbagbuddy.R
import com.example.yourbagbuddy.presentation.ui.theme.Primary
import com.example.yourbagbuddy.presentation.ui.theme.Success
import com.example.yourbagbuddy.presentation.viewmodel.SignupViewModel

@Composable
private fun PasswordRequirementChecklist(
    hasMinLength: Boolean,
    hasNumber: Boolean,
    hasSymbol: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PasswordRequirementRow(
            label = "At least 6 characters",
            met = hasMinLength
        )
        PasswordRequirementRow(
            label = "At least 1 number",
            met = hasNumber
        )
        PasswordRequirementRow(
            label = "At least 1 symbol",
            met = hasSymbol
        )
    }
}

@Composable
private fun PasswordRequirementRow(
    label: String,
    met: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (met) 1f else 0.85f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (met) 1f else 0.6f,
        animationSpec = tween(durationMillis = 200),
        label = "alpha"
    )
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedContent(
            targetState = met,
            transitionSpec = {
                (scaleIn(initialScale = 0.8f) + fadeIn()).togetherWith(
                    fadeOut() + scaleOut(targetScale = 0.8f)
                )
            },
            label = "check"
        ) { isMet ->
            if (isMet) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Success
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (met) Success else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SignupScreen(
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onProfileSaved: () -> Unit = {},
    viewModel: SignupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val profileSaved by viewModel.profileSaved.collectAsState()

    LaunchedEffect(profileSaved) {
        if (profileSaved) {
            onProfileSaved()
            viewModel.clearProfileSaved()
        }
    }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.signup_traveller)
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    val infiniteTransition = rememberInfiniteTransition(label = "signupShimmer")
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
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToLogin) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to login"
                )
            }
            TextButton(onClick = onNavigateToHome) {
                Text(
                    "Skip",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LottieAnimation(
            composition = composition,
            progress = { lottieProgress },
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    scaleX = subtleScale
                    scaleY = subtleScale
                }
        )

        Text(
            text = "Complete your profile",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Your data is saved securely in the cloud",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

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
                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = Primary,
                    cursorColor = Primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
                var passwordVisible by remember { mutableStateOf(false) }
                var confirmPasswordVisible by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = viewModel::updateDisplayName,
                    label = { Text("Name") },
                    placeholder = { Text("Your name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(16.dp),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text("Email") },
                    placeholder = { Text("name@example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(16.dp),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Password") },
                    placeholder = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = textFieldColors
                )

                if (state.password.isNotEmpty()) {
                    PasswordRequirementChecklist(
                        hasMinLength = state.passwordHasMinLength,
                        hasNumber = state.passwordHasNumber,
                        hasSymbol = state.passwordHasSymbol
                    )
                }

                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::updateConfirmPassword,
                    label = { Text("Confirm password") },
                    placeholder = { Text("Re-enter password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = state.phoneNumber,
                    onValueChange = viewModel::updatePhoneNumber,
                    label = { Text("Phone number (optional)") },
                    placeholder = { Text("+1 234 567 8900") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp),
                    colors = textFieldColors
                )

                state.error?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = { viewModel.saveProfile() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            "Save profile",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(
                "Already have an account? ",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Sign in",
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
