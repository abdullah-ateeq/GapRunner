package com.example.gaprunner.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Game : Screen("game")
    data object Result : Screen("result/{score}/{obstacles}/{streak}") {
        fun createRoute(score: Int, obstacles: Int, streak: Int): String {
            return "result/$score/$obstacles/$streak"
        }
    }
    data object Settings : Screen("settings")
    data object About : Screen("about")
}
