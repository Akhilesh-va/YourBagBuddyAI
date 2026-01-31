package com.example.yourbagbuddy.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import android.net.Uri
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.yourbagbuddy.presentation.screen.ChatScreen
import com.example.yourbagbuddy.presentation.screen.ChecklistScreen
import com.example.yourbagbuddy.presentation.screen.HomeScreen
import com.example.yourbagbuddy.presentation.screen.LoginScreen
import com.example.yourbagbuddy.presentation.screen.SignupScreen
import com.example.yourbagbuddy.presentation.screen.SettingsScreen
import com.example.yourbagbuddy.presentation.screen.SmartPackScreen
import com.example.yourbagbuddy.presentation.screen.TravelMedicineScreen
import com.example.yourbagbuddy.presentation.screen.TripsScreen
import com.example.yourbagbuddy.presentation.viewmodel.SmartPackViewModel

/** Builds login route with optional return destination (bottom nav tab to open after sign-in). */
fun loginRoute(returnTo: String = Screen.Home.route) = "login?returnTo=$returnTo"

/** Builds signup route with optional return destination (bottom nav tab to open after sign-up). */
fun signupRoute(returnTo: String = Screen.Home.route) = "signup?returnTo=$returnTo"

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Checklist : Screen("checklist") // "Your Checklist"
    object BestChoices : Screen("best_choices") // "Ai choices" (AI feature)
    object Profile : Screen("profile") // "Profile"
    object Trips : Screen("trips") // Keep for internal navigation
    object SmartPack : Screen("smart_pack") // Keep for internal navigation
    object Settings : Screen("settings") // Keep for internal navigation
    object AiList : Screen("ai_list") // AI-generated packing list screen
    object Chat : Screen("chat") // Packing assistant chatbot (same AI as list)
    object TripDetail : Screen("trip_detail/{tripId}") {
        fun createRoute(tripId: String) = "trip_detail/$tripId"
    }
    object TravelMedicine : Screen("travel_medicine")
}

fun checklistRoute(joinCode: String? = null): String =
    if (joinCode != null) "checklist?joinCode=${Uri.encode(joinCode)}" else Screen.Checklist.route

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    initialTripIdToOpen: String? = null,
    onClearInitialTripIdToOpen: () -> Unit = {},
    initialJoinCode: String? = null,
    onClearInitialJoinCode: () -> Unit = {}
) {
    // When opened from packing reminder notification, navigate to that trip’s checklist.
    LaunchedEffect(initialTripIdToOpen) {
        if (initialTripIdToOpen != null) {
            navController.navigate(Screen.TripDetail.createRoute(initialTripIdToOpen)) {
                launchSingleTop = true
                popUpTo(Screen.Home.route) { inclusive = false }
            }
            onClearInitialTripIdToOpen()
        }
    }
    LaunchedEffect(initialJoinCode) {
        if (initialJoinCode != null) {
            navController.navigate(checklistRoute(initialJoinCode)) {
                launchSingleTop = true
                popUpTo(Screen.Home.route) { inclusive = false }
            }
            onClearInitialJoinCode()
        }
    }
    NavHost(
        navController = navController,
        // Users land on Home after splash, regardless of auth state.
        // Auth is required only when using AI-related features.
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(
            route = "login?returnTo={returnTo}",
            arguments = listOf(
                navArgument("returnTo") { type = NavType.StringType; defaultValue = Screen.Home.route }
            )
        ) { backStackEntry ->
            val returnTo = backStackEntry.arguments?.getString("returnTo") ?: Screen.Home.route
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(returnTo) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToSignUp = {
                    navController.popBackStack()
                    navController.navigate(signupRoute(returnTo))
                },
                onSignedIn = { user ->
                    if (user.hasCompleteProfile) {
                        navController.navigate(returnTo) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.popBackStack()
                        navController.navigate(signupRoute(returnTo))
                    }
                }
            )
        }
        composable(
            route = "signup?returnTo={returnTo}",
            arguments = listOf(
                navArgument("returnTo") { type = NavType.StringType; defaultValue = Screen.Home.route }
            )
        ) { backStackEntry ->
            val returnTo = backStackEntry.arguments?.getString("returnTo") ?: Screen.Home.route
            SignupScreen(
                onNavigateToHome = {
                    navController.navigate(returnTo) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                    navController.navigate(loginRoute(returnTo))
                },
                onProfileSaved = {
                    navController.navigate(returnTo) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTrips = { navController.navigate(Screen.Checklist.route) },
                onNavigateToCreateTrip = { navController.navigate(Screen.Checklist.route) },
                onNavigateToTravelMedicine = { navController.navigate(Screen.TravelMedicine.route) }
            )
        }
        // Bottom Navigation Screens
        composable(
            route = "checklist?joinCode={joinCode}",
            arguments = listOf(
                navArgument("joinCode") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val initialJoinCode = backStackEntry.arguments?.getString("joinCode")
            ChecklistScreen(
                onNavigateBack = { navController.popBackStack() },
                initialJoinCode = initialJoinCode,
                onConsumeJoinCode = {
                    navController.navigate(Screen.Checklist.route) {
                        popUpTo(Screen.Checklist.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.BestChoices.route) {
            SmartPackScreen(
                onNavigateToAiList = { navController.navigate(Screen.AiList.route) },
                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                onNavigateToLogin = {
                    navController.navigate(loginRoute(Screen.BestChoices.route))
                },
                onNavigateToSignUp = {
                    navController.navigate(signupRoute(Screen.BestChoices.route))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onDarkThemeChange = onDarkThemeChange,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(loginRoute(Screen.Profile.route)) },
                onNavigateToSignUp = { navController.navigate(signupRoute(Screen.Profile.route)) },
                onNavigateToTravelMedicine = { navController.navigate(Screen.TravelMedicine.route) }
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
                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                onNavigateToLogin = {
                    navController.navigate(loginRoute(Screen.BestChoices.route))
                },
                onNavigateToSignUp = {
                    navController.navigate(signupRoute(Screen.BestChoices.route))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onDarkThemeChange = onDarkThemeChange,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(loginRoute(Screen.Profile.route)) },
                onNavigateToSignUp = { navController.navigate(signupRoute(Screen.Profile.route)) },
                onNavigateToTravelMedicine = { navController.navigate(Screen.TravelMedicine.route) }
            )
        }
        composable(Screen.TripDetail.route) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            com.example.yourbagbuddy.presentation.screen.TripDetailScreen(
                tripId = tripId,
                onNavigateBack = { navController.popBackStack() },
                onDuplicateSuccess = { newTripId ->
                    navController.navigate(Screen.TripDetail.createRoute(newTripId)) {
                        popUpTo(Screen.TripDetail.createRoute(tripId)) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.AiList.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.BestChoices.route)
            }
            val smartPackViewModel: SmartPackViewModel = hiltViewModel(parentEntry)
            com.example.yourbagbuddy.presentation.screen.AiListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChecklist = {
                    navController.navigate(Screen.Checklist.route) {
                        popUpTo(Screen.AiList.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                viewModel = smartPackViewModel
            )
        }
        composable(Screen.Chat.route) {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(loginRoute(Screen.Chat.route))
                },
                onNavigateToSignUp = {
                    navController.navigate(signupRoute(Screen.Chat.route))
                }
            )
        }
        composable(Screen.TravelMedicine.route) {
            TravelMedicineScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
