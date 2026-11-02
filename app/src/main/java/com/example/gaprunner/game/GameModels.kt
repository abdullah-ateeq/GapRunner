package com.example.gaprunner.game

/**
 * Difficulty tier based on score or obstacles passed.
 */
enum class Difficulty(val label: String, val speedMultiplier: Float) {
    EASY("Easy", 1.0f),
    MEDIUM("Medium", 1.28f),
    HARD("Hard", 1.62f)
}

/**
 * Configuration parameters for the GapRunner game logic.
 * All spatial dimensions are normalized (0f to 1f).
 */
data class GameConfig(
    val playerWidth: Float = 0.075f,
    val playerHeight: Float = 0.038f,
    val playerY: Float = 0.82f,
    val obstacleThickness: Float = 0.032f,
    val baseObstacleSpeed: Float = 0.28f, // Screen heights per second
    val maxObstacleSpeed: Float = 0.58f,
    val minGapWidth: Float = 0.135f,
    val maxGapWidth: Float = 0.28f,
    val borderMargin: Float = 0.04f,
    val hitboxForgiveness: Float = 0.15f // 15% inner grace margin on player width
)

/**
 * Representation of an obstacle spanning the playfield with a single safe gap.
 */
data class Obstacle(
    val id: Long,
    val y: Float,
    val gapLeft: Float,
    val gapWidth: Float,
    val thickness: Float = 0.032f,
    val passed: Boolean = false,
    val highlightProgress: Float = 0f // 1f -> 0f flash effect on successful pass
) {
    val gapRight: Float get() = gapLeft + gapWidth
    val gapCenter: Float get() = gapLeft + (gapWidth / 2f)

    fun isCompletelyOffscreen(): Boolean = y - (thickness / 2f) > 1.15f
}

/**
 * Trail point for the runner's visual feedback.
 */
data class TrailPoint(
    val x: Float,
    val y: Float,
    val alpha: Float
)

/**
 * Floating feedback text when scoring (e.g. "+120", "STREAK x3", "PERFECT!").
 */
data class ScorePopFeedback(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val alpha: Float = 1f,
    val isPerfect: Boolean = false
)

/**
 * Game execution state.
 */
enum class GamePlayState {
    READY,
    RUNNING,
    PAUSED,
    GAME_OVER
}

/**
 * Complete immutable state of the game world.
 */
data class GameState(
    val playState: GamePlayState = GamePlayState.READY,
    val playerX: Float = 0.5f,
    val playerY: Float = 0.82f,
    val playerVelocityX: Float = 0f,
    val obstacles: List<Obstacle> = emptyList(),
    val trail: List<TrailPoint> = emptyList(),
    val scorePops: List<ScorePopFeedback> = emptyList(),
    val score: Int = 0,
    val currentStreak: Int = 0,
    val highestStreakThisRun: Int = 0,
    val obstaclesPassed: Int = 0,
    val difficulty: Difficulty = Difficulty.EASY,
    val currentSpeed: Float = 0.28f,
    val screenShakeIntensity: Float = 0f,
    val collisionImpactX: Float? = null,
    val collisionImpactY: Float? = null,
    val timeElapsedSeconds: Float = 0f
)
