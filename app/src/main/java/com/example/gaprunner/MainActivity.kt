package com.example.gaprunner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gaprunner.audio.HapticFeedbackManager
import com.example.gaprunner.audio.SoundEffectManager
import com.example.gaprunner.data.UserPreferencesRepository
import com.example.gaprunner.ui.navigation.Screen
import com.example.gaprunner.ui.screens.AboutScreen
import com.example.gaprunner.ui.screens.GameScreen
import com.example.gaprunner.ui.screens.HomeScreen
import com.example.gaprunner.ui.screens.ResultScreen
import com.example.gaprunner.ui.screens.SettingsScreen
import com.example.gaprunner.ui.screens.SplashScreen
import com.example.gaprunner.ui.theme.GapRunnerTheme
import com.example.gaprunner.ui.viewmodel.GameViewModel
import com.example.gaprunner.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencesRepository = UserPreferencesRepository(applicationContext)
        val soundManager = SoundEffectManager()
        val hapticManager = HapticFeedbackManager(applicationContext)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SettingsViewModel(preferencesRepository) as T
                    }
                }
            )

            val gameViewModel: GameViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return GameViewModel(preferencesRepository, soundManager, hapticManager) as T
                    }
                }
            )

            val preferences by settingsViewModel.preferences.collectAsState()

            GapRunnerTheme(appTheme = preferences.theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GapRunnerApp(
                        gameViewModel = gameViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun GapRunnerApp(
    gameViewModel: GameViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                preferencesFlow = settingsViewModel.preferences,
                onPlayClick = {
                    gameViewModel.startGame()
                    navController.navigate(Screen.Game.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onAboutClick = {
                    navController.navigate(Screen.About.route)
                }
            )
        }

        composable(Screen.Game.route) {
            GameScreen(
                viewModel = gameViewModel,
                onGameOver = { score, obstacles, streak ->
                    navController.navigate(Screen.Result.createRoute(score, obstacles, streak)) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("score") { type = NavType.IntType },
                navArgument("obstacles") { type = NavType.IntType },
                navArgument("streak") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val obstacles = backStackEntry.arguments?.getInt("obstacles") ?: 0
            val streak = backStackEntry.arguments?.getInt("streak") ?: 0

            ResultScreen(
                score = score,
                obstacles = obstacles,
                streak = streak,
                preferencesFlow = settingsViewModel.preferences,
                onPlayAgainClick = {
                    gameViewModel.restartGame()
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
