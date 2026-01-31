package com.example.yourbagbuddy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.example.yourbagbuddy.presentation.navigation.FloatedBottomNavigationBar
import com.example.yourbagbuddy.presentation.navigation.NavGraph
import com.example.yourbagbuddy.presentation.navigation.Screen
import com.example.yourbagbuddy.presentation.screen.SplashScreen
import com.example.yourbagbuddy.presentation.ui.theme.YourBagBuddyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    /** When the app is opened via yourbagbuddy://join/CODE (or brought to foreground with that intent), this holds the code until consumed. */
    private val pendingJoinCodeFlow = MutableStateFlow<String?>(null)

    companion object {
        /** Intent extra set by packing reminder notification; open app to this trip’s checklist. */
        const val EXTRA_OPEN_TRIP_ID = "com.example.yourbagbuddy.OPEN_TRIP_ID"
        const val JOIN_LINK_SCHEME = "yourbagbuddy"
        const val JOIN_LINK_HOST = "join"
        /** HTTPS join link base (clickable in WhatsApp/SMS). Path: /join/CODE */
        const val JOIN_LINK_HTTPS_HOST = "yourbagbuddy-ai.firebaseapp.com"
        fun buildJoinLinkHttps(inviteCode: String): String =
            "https://$JOIN_LINK_HTTPS_HOST/join/$inviteCode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        when {
            data != null && parseJoinCodeFromIntent(intent) != null ->
                pendingJoinCodeFlow.value = parseJoinCodeFromIntent(intent)
            data != null -> authRepository.storePendingEmailLinkIfSignInLink(data.toString())
            else -> Unit
        }
        enableEdgeToEdge()
        setContent {
            // Initialize theme based on the current system setting,
            // but allow the user to override it from the Settings screen.
            val systemPrefersDark = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable { mutableStateOf(systemPrefersDark) }
            var showSplash by rememberSaveable { mutableStateOf(true) }

            YourBagBuddyTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Show bottom navigation only on primary destinations (checklist includes checklist?joinCode=...)
                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.BestChoices.route,
                    Screen.Profile.route
                ) || currentRoute == Screen.Checklist.route || (currentRoute?.startsWith("checklist") == true)

                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "splash_to_main_transition"
                ) { isSplashVisible ->
                    if (isSplashVisible) {
                        SplashScreen(
                            onNavigateNext = { showSplash = false }
                        )
                    } else {
                        var openTripIdFromNotification by remember { mutableStateOf(intent?.getStringExtra(EXTRA_OPEN_TRIP_ID)) }
                        val pendingJoinCode by pendingJoinCodeFlow.collectAsState(initial = null)
                        Box(modifier = Modifier.fillMaxSize()) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = {
                                    if (showBottomBar) {
                                        FloatedBottomNavigationBar(navController = navController)
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.background
                            ) { innerPadding ->
                                NavGraph(
                                    navController = navController,
                                    isDarkTheme = isDarkTheme,
                                    onDarkThemeChange = { isDarkTheme = it },
                                    initialTripIdToOpen = openTripIdFromNotification,
                                    onClearInitialTripIdToOpen = { openTripIdFromNotification = null },
                                    initialJoinCode = pendingJoinCode,
                                    onClearInitialJoinCode = { pendingJoinCodeFlow.value = null },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                )
                            }
                            // Floating chat icon – visible only on Ai choices screen
                            if (currentRoute == Screen.BestChoices.route) {
                                FloatingActionButton(
                                    onClick = {
                                        navController.navigate(Screen.Chat.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 24.dp, bottom = 92.dp),
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = "Chat with YourBagBuddy",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val data = intent.data
        when {
            data != null && parseJoinCodeFromIntent(intent) != null ->
                pendingJoinCodeFlow.value = parseJoinCodeFromIntent(intent)
            data != null -> authRepository.storePendingEmailLinkIfSignInLink(data.toString())
            else -> Unit
        }
    }

    private fun parseJoinCodeFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return when {
            data.scheme == JOIN_LINK_SCHEME && data.host == JOIN_LINK_HOST ->
                data.path?.trimStart('/')?.takeIf { it.isNotBlank() }
            data.scheme == "https" && data.host == JOIN_LINK_HTTPS_HOST && data.path?.startsWith("/join") == true ->
                data.path?.removePrefix("/join")?.trimStart('/')?.takeIf { it.isNotBlank() }
            else -> null
        }
    }
}