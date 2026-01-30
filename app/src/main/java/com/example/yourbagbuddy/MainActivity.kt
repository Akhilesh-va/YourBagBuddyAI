package com.example.yourbagbuddy

import android.content.Intent
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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    companion object {
        /** Intent extra set by packing reminder notification; open app to this trip’s checklist. */
        const val EXTRA_OPEN_TRIP_ID = "com.example.yourbagbuddy.OPEN_TRIP_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.toString()?.let { link ->
            authRepository.storePendingEmailLinkIfSignInLink(link)
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

                // Show bottom navigation only on primary destinations
                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Checklist.route,
                    Screen.BestChoices.route,
                    Screen.Profile.route
                )

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
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = innerPadding.calculateBottomPadding())
                                )
                            }
                            // Floating chat icon – visible only on Best Choices screen
                            if (currentRoute == Screen.BestChoices.route) {
                                FloatingActionButton(
                                    onClick = {
                                        navController.navigate(Screen.Chat.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 24.dp, bottom = 100.dp),
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
        intent.data?.toString()?.let { link ->
            authRepository.storePendingEmailLinkIfSignInLink(link)
        }
    }
}