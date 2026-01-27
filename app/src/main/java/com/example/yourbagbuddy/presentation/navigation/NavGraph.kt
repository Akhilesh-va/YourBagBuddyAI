package com.example.yourbagbuddy.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.yourbagbuddy.presentation.screen.ChecklistScreen
import com.example.yourbagbuddy.presentation.screen.HomeScreen
import com.example.yourbagbuddy.presentation.screen.LoginScreen
import com.example.yourbagbuddy.presentation.screen.SignupScreen
import com.example.yourbagbuddy.presentation.screen.SettingsScreen
import com.example.yourbagbuddy.presentation.screen.SmartPackScreen
import com.example.yourbagbuddy.presentation.screen.TripsScreen
import com.example.yourbagbuddy.presentation.viewmodel.SmartPackViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Checklist : Screen("checklist") // "Your Checklist"
    object BestChoices : Screen("best_choices") // "Best Choices" (AI feature)
    object Profile : Screen("profile") // "Profile"
    object Trips : Screen("trips") // Keep for internal navigation
    object SmartPack : Screen("smart_pack") // Keep for internal navigation
    object Settings : Screen("settings") // Keep for internal navigation
    object AiList : Screen("ai_list") // AI-generated packing list screen
    object TripDetail : Screen("trip_detail/{tripId}") {
        fun createRoute(tripId: String) = "trip_detail/$tripId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onSignedIn = { user ->
                    if (user.hasCompleteProfile) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.SignUp.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.SignUp.route) {
            SignupScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onProfileSaved = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTrips = { navController.navigate(Screen.Checklist.route) },
                onNavigateToCreateTrip = { navController.navigate(Screen.Checklist.route) }
            )
        }
        // Bottom Navigation Screens
        composable(Screen.Checklist.route) {
            ChecklistScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.BestChoices.route) {
            SmartPackScreen(
                onNavigateToAiList = { navController.navigate(Screen.AiList.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onDarkThemeChange = onDarkThemeChange,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // Internal navigation screens (not in bottom nav)
        composable(Screen.Trips.route) {
            TripsScreen(
                onNavigateToTripDetail = { tripId ->
                    navController.navigate(Screen.TripDetail.createRoute(tripId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SmartPack.route) {
            SmartPackScreen(
                onNavigateToAiList = { navController.navigate(Screen.AiList.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onDarkThemeChange = onDarkThemeChange,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TripDetail.route) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            com.example.yourbagbuddy.presentation.screen.TripDetailScreen(
                tripId = tripId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiList.route) {
            val parentEntry = remember(navController) {
                navController.getBackStackEntry(Screen.BestChoices.route)
            }
            val smartPackViewModel: SmartPackViewModel = hiltViewModel(parentEntry)
            com.example.yourbagbuddy.presentation.screen.AiListScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = smartPackViewModel
            )
        }
    }
}
