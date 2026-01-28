package com.example.yourbagbuddy.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.yourbagbuddy.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onNavigateNext: () -> Unit
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.splash_traveller)
    )

    // Keep splash for a fixed 3 seconds
    LaunchedEffect(Unit) {
        delay(4000)
        onNavigateNext()
    }

    // Scale + alpha animation for the title text
    val infiniteTransition = rememberInfiniteTransition(label = "splashTextAnim")
    val textScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textScale"
    )
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    val logoFontFamily = FontFamily(Font(R.font.splash_logo_text_font))

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        composition?.let {
            LottieAnimation(
                composition = it,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(320.dp)
            )

            Text(
                text = "You Bag Buddy",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = logoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    color = Color(0xFF006837),

                ),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .graphicsLayer(
                        scaleX = textScale,
                        scaleY = textScale,
                        alpha = textAlpha
                    )
            )
        }
    }
}

