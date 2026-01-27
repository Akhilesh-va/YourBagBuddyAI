package com.example.yourbagbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.yourbagbuddy.presentation.navigation.FloatedBottomNavigationBar
import com.example.yourbagbuddy.presentation.navigation.NavGraph
import com.example.yourbagbuddy.presentation.navigation.Screen
import com.example.yourbagbuddy.presentation.screen.SplashScreen
import com.example.yourbagbuddy.presentation.ui.theme.YourBagBuddyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding())
                            )
                        }
                    }
                }
            }
        }
    }
}