package com.example.gaprunner.data

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val bestScore: Int = 0,
    val highestStreak: Int = 0,
    val totalGamesPlayed: Int = 0
)
