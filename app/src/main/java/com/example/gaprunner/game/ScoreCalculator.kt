package com.example.gaprunner.game

import kotlin.math.abs
import kotlin.math.min

data class PassEvaluation(
    val pointsAwarded: Int,
    val streak: Int,
    val isPerfectCenter: Boolean,
    val streakMultiplier: Float,
    val feedbackLabel: String
)

object ScoreCalculator {

    const val BASE_PASS_POINTS = 100
    const val PERFECT_CENTER_BONUS = 50

    /**
     * Determines streak multiplier based on streak count.
     */
    fun getStreakMultiplier(streak: Int): Float {
        return when {
            streak < 3 -> 1.0f
            streak < 6 -> 1.25f
            streak < 10 -> 1.5f
            streak < 15 -> 2.0f
            streak < 20 -> 2.5f
            else -> 3.0f
        }
    }

    /**
     * Evaluates a successful obstacle pass, awarding points, streak, and center bonus.
     */
    fun evaluatePass(
        playerX: Float,
        obstacle: Obstacle,
        currentStreak: Int
    ): PassEvaluation {
        val newStreak = currentStreak + 1
        val multiplier = getStreakMultiplier(newStreak)

        // Precision check: distance from player center to gap center
        val distanceToCenter = abs(playerX - obstacle.gapCenter)
        val maxPermittedDistance = (obstacle.gapWidth / 2f) * 0.30f
        val isPerfect = distanceToCenter <= maxPermittedDistance

        val baseEarned = (BASE_PASS_POINTS * multiplier).toInt()
        val totalEarned = if (isPerfect) baseEarned + PERFECT_CENTER_BONUS else baseEarned

        val label = when {
            isPerfect && newStreak >= 5 -> "PERFECT! x${multiplier} (+$totalEarned)"
            isPerfect -> "PERFECT! (+$totalEarned)"
            newStreak >= 5 -> "STREAK x${multiplier} (+$totalEarned)"
            else -> "+$totalEarned"
        }

        return PassEvaluation(
            pointsAwarded = totalEarned,
            streak = newStreak,
            isPerfectCenter = isPerfect,
            streakMultiplier = multiplier,
            feedbackLabel = label
        )
    }

    /**
     * Computes the current difficulty based on obstacles passed.
     */
    fun determineDifficulty(obstaclesPassed: Int): Difficulty {
        return when {
            obstaclesPassed < 6 -> Difficulty.EASY
            obstaclesPassed < 16 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }
    }

    /**
     * Calculates speed for the obstacles given obstacles passed.
     */
    fun calculateSpeed(
        obstaclesPassed: Int,
        config: GameConfig = GameConfig()
    ): Float {
        val progress = min(1.0f, obstaclesPassed / 25f)
        val speedRange = config.maxObstacleSpeed - config.baseObstacleSpeed
        return config.baseObstacleSpeed + (speedRange * progress)
    }
}
