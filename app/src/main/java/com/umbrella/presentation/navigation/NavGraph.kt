package com.umbrella.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.umbrella.presentation.main.MainScreen
import com.umbrella.presentation.main.MainUiState
import com.umbrella.presentation.onboarding.OnboardingScreen
import com.umbrella.presentation.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

@Composable
fun UmbrellaNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    mainUiState: MainUiState,
    onRefresh: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    onCompleteOnboarding()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestLocationPermission = onRequestLocationPermission
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                uiState = mainUiState,
                onRefresh = onRefresh,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestLocationPermission = onRequestLocationPermission,
                onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
