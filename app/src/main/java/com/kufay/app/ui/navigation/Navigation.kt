package com.kufay.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kufay.app.ui.screens.HomeScreen
import com.kufay.app.ui.screens.SettingsScreen
import com.kufay.app.ui.screens.TrashScreen
import com.kufay.app.ui.viewmodels.HomeViewModel
import com.kufay.app.ui.viewmodels.SettingsViewModel
import com.kufay.app.ui.viewmodels.TrashViewModel
import com.kufay.app.ui.screens.PinScreen
import com.kufay.app.ui.viewmodels.PinViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kufay.app.ui.screens.WelcomeScreen
import com.kufay.app.ui.viewmodels.WelcomeViewModel

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome") // Add this line
    object Pin : Screen("pin") // Add this line
    object Home : Screen("home")
    object Settings : Screen("settings")
    object Trash : Screen("trash")
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Welcome.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Welcome Screen - the new first screen for new users
        composable(Screen.Welcome.route) {
            val viewModel: WelcomeViewModel = hiltViewModel()

            WelcomeScreen(
                viewModel = viewModel,
                onContinue = {
                    navController.navigate(Screen.Pin.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        // Add PIN Screen route
        composable(Screen.Pin.route) {
            val viewModel: PinViewModel = hiltViewModel()
            PinScreen(
                viewModel = viewModel,
                onAuthenticated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Pin.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                homeViewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) },
                onNavigateToTrash = {
                    navController.navigate(Screen.Trash.route) }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()

            // Observe navigation state
            val navigateToPinSetup by viewModel.navigateToPinSetup.collectAsState()

            // Handle navigation to PIN setup screen
            LaunchedEffect(navigateToPinSetup) {
                if (navigateToPinSetup) {
                    navController.navigate(Screen.Pin.route)
                    viewModel.resetNavigateToPinSetup()
                }
            }

            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Trash.route) {
            val viewModel: TrashViewModel = hiltViewModel()
            TrashScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Pin.route) {
            val viewModel: PinViewModel = hiltViewModel()

            PinScreen(
                viewModel = viewModel,
                onAuthenticated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Pin.route) { inclusive = true }
                    }
                }
            )
        }
    }
}