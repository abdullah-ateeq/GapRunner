package com.example.gaprunner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gaprunner_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("app_theme")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val BEST_SCORE = intPreferencesKey("best_score")
        val HIGHEST_STREAK = intPreferencesKey("highest_streak")
        val TOTAL_GAMES_PLAYED = intPreferencesKey("total_games_played")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeString = preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
            val theme = try {
                AppTheme.valueOf(themeString)
            } catch (e: IllegalArgumentException) {
                AppTheme.SYSTEM
            }
            val soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true
            val hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true
            val bestScore = preferences[PreferencesKeys.BEST_SCORE] ?: 0
            val highestStreak = preferences[PreferencesKeys.HIGHEST_STREAK] ?: 0
            val totalGamesPlayed = preferences[PreferencesKeys.TOTAL_GAMES_PLAYED] ?: 0

            UserPreferences(
                theme = theme,
                soundEnabled = soundEnabled,
                hapticsEnabled = hapticsEnabled,
                bestScore = bestScore,
                highestStreak = highestStreak,
                totalGamesPlayed = totalGamesPlayed
            )
        }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun recordRunResults(score: Int, streak: Int) {
        context.dataStore.edit { preferences ->
            val currentBest = preferences[PreferencesKeys.BEST_SCORE] ?: 0
            val currentHighestStreak = preferences[PreferencesKeys.HIGHEST_STREAK] ?: 0
            val games = preferences[PreferencesKeys.TOTAL_GAMES_PLAYED] ?: 0

            if (score > currentBest) {
                preferences[PreferencesKeys.BEST_SCORE] = score
            }
            if (streak > currentHighestStreak) {
                preferences[PreferencesKeys.HIGHEST_STREAK] = streak
            }
            preferences[PreferencesKeys.TOTAL_GAMES_PLAYED] = games + 1
        }
    }

    suspend fun resetScores() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BEST_SCORE] = 0
            preferences[PreferencesKeys.HIGHEST_STREAK] = 0
        }
    }
}
