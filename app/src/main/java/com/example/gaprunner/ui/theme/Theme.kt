package com.example.gaprunner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.gaprunner.data.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = ArcadeAccentCyan,
    onPrimary = ArcadeDarkBg,
    primaryContainer = ArcadeDarkSurfaceVariant,
    onPrimaryContainer = ArcadeAccentCyan,
    secondary = ArcadeAccentBlue,
    onSecondary = ArcadeDarkBg,
    background = ArcadeDarkBg,
    onBackground = TextPrimaryDark,
    surface = ArcadeDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ArcadeDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = ArcadeDarkBorder,
    error = ArcadeImpactRed
)

private val LightColorScheme = lightColorScheme(
    primary = ArcadeAccentLight,
    onPrimary = ArcadeLightSurface,
    primaryContainer = ArcadeLightSurfaceVariant,
    onPrimaryContainer = ArcadeAccentLightDim,
    secondary = ArcadeAccentBlue,
    onSecondary = ArcadeLightSurface,
    background = ArcadeLightBg,
    onBackground = TextPrimaryLight,
    surface = ArcadeLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = ArcadeLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = ArcadeLightBorder,
    error = ArcadeImpactRed
)

@Composable
fun GapRunnerTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
