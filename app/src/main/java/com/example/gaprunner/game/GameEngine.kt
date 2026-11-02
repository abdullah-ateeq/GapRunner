package com.example.gaprunner.game

import kotlin.math.max
import kotlin.random.Random

data class GameEvents(
    val obstaclePassed: Boolean = false,
    val isPerfect: Boolean = false,
    val hasCollided: Boolean = false,
    val collisionPoint: Pair<Float, Float>? = null
)

class GameEngine(
    val config: GameConfig = GameConfig(),
    private var nextObstacleId: Long = 1L
) {

    fun createInitialState(random: Random = Random.Default): GameState {
        nextObstacleId = 1L
        // Pre-generate 3 obstacles ahead
        val initialObstacles = mutableListOf<Obstacle>()
        var currentY = 0.15f
        var lastGapCenter: Float? = null

        repeat(3) {
            val obstacle = GapGenerator.generateNextObstacle(
                id = nextObstacleId++,
                y = currentY,
                previousGapCenter = lastGapCenter,
                obstaclesPassed = 0,
                config = config,
                random = random
            )
            initialObstacles.add(obstacle)
            lastGapCenter = obstacle.gapCenter
            currentY -= GapGenerator.calculateObstacleSpacing(0)
        }

        return GameState(
            playState = GamePlayState.READY,
            playerX = 0.5f,
            playerY = config.playerY,
            obstacles = initialObstacles,
            currentSpeed = config.baseObstacleSpeed
        )
    }

    /**
     * Updates the game state by delta time [dt] in seconds.
     */
    fun update(
        state: GameState,
        dt: Float,
        random: Random = Random.Default
    ): Pair<GameState, GameEvents> {
        if (state.playState != GamePlayState.RUNNING) {
            return state to GameEvents()
        }

        val clampedDt = dt.coerceIn(0f, 0.1f) // Guard against spiral-of-death lag spikes
        var events = GameEvents()

        // 1. Update player horizontal position if velocity is present
        var newPlayerX = state.playerX
        if (state.playerVelocityX != 0f) {
            newPlayerX = GameCollision.clampPlayerX(newPlayerX + state.playerVelocityX * clampedDt, config)
        }

        // 2. Update speed and difficulty
        val difficulty = ScoreCalculator.determineDifficulty(state.obstaclesPassed)
        val speed = ScoreCalculator.calculateSpeed(state.obstaclesPassed, config)

        // 3. Move obstacles downward and check collision / pass
        var currentScore = state.score
        var currentStreak = state.currentStreak
        var highestStreak = state.highestStreakThisRun
        var obstaclesPassed = state.obstaclesPassed
        val newScorePops = state.scorePops.mapNotNull { pop ->
            val nextAlpha = pop.alpha - (clampedDt * 1.5f)
            if (nextAlpha > 0f) {
                pop.copy(y = pop.y - (clampedDt * 0.08f), alpha = nextAlpha)
            } else null
        }.toMutableList()

        val updatedObstacles = mutableListOf<Obstacle>()
        var hasCollided = false
        var collisionPoint: Pair<Float, Float>? = null

        for (obstacle in state.obstacles) {
            val newY = obstacle.y + (speed * clampedDt)
            var highlight = max(0f, obstacle.highlightProgress - (clampedDt * 3.0f))
            var isPassed = obstacle.passed

            val movedObstacle = obstacle.copy(y = newY, highlightProgress = highlight)

            // Collision check
            if (!isPassed && GameCollision.isColliding(newPlayerX, state.playerY, movedObstacle, config)) {
                hasCollided = true
                collisionPoint = newPlayerX to state.playerY
            }

            // Pass check
            if (!isPassed && !hasCollided && GameCollision.hasPassedPlayer(state.playerY, movedObstacle)) {
                isPassed = true
                highlight = 1.0f
                obstaclesPassed++
                val passEval = ScoreCalculator.evaluatePass(newPlayerX, movedObstacle, currentStreak)
                currentScore += passEval.pointsAwarded
                currentStreak = passEval.streak
                if (currentStreak > highestStreak) {
                    highestStreak = currentStreak
                }

                events = events.copy(
                    obstaclePassed = true,
                    isPerfect = passEval.isPerfectCenter
                )

                newScorePops.add(
                    ScorePopFeedback(
                        id = System.nanoTime(),
                        text = passEval.feedbackLabel,
                        x = newPlayerX,
                        y = state.playerY - 0.06f,
                        isPerfect = passEval.isPerfectCenter
                    )
                )
            }

            // Only keep if not off screen
            if (!movedObstacle.isCompletelyOffscreen()) {
                updatedObstacles.add(movedObstacle.copy(passed = isPassed, highlightProgress = highlight))
            }
        }

        // 4. Spawn new obstacles if needed at top
        val highestObstacle = updatedObstacles.minByOrNull { it.y }
        val targetSpacing = GapGenerator.calculateObstacleSpacing(obstaclesPassed)
        if (highestObstacle == null || highestObstacle.y > -0.1f) {
            val spawnY = (highestObstacle?.y ?: 0f) - targetSpacing
            val newObstacle = GapGenerator.generateNextObstacle(
                id = nextObstacleId++,
                y = spawnY,
                previousGapCenter = highestObstacle?.gapCenter,
                obstaclesPassed = obstaclesPassed,
                config = config,
                random = random
            )
            updatedObstacles.add(newObstacle)
        }

        // 5. Update trail
        val updatedTrail = (state.trail.mapNotNull { pt ->
            val nextAlpha = pt.alpha - (clampedDt * 2.8f)
            if (nextAlpha > 0f) pt.copy(alpha = nextAlpha) else null
        } + TrailPoint(newPlayerX, state.playerY, 0.7f)).takeLast(8)

        // 6. Screen shake update
        val newShake = if (hasCollided) 1.0f else max(0f, state.screenShakeIntensity - (clampedDt * 2.5f))

        val finalPlayState = if (hasCollided) GamePlayState.GAME_OVER else state.playState
        if (hasCollided) {
            events = events.copy(hasCollided = true, collisionPoint = collisionPoint)
        }

        val nextState = state.copy(
            playState = finalPlayState,
            playerX = newPlayerX,
            playerVelocityX = state.playerVelocityX * 0.85f, // Smooth dampening
            obstacles = updatedObstacles,
            trail = updatedTrail,
            scorePops = newScorePops,
            score = currentScore,
            currentStreak = currentStreak,
            highestStreakThisRun = highestStreak,
            obstaclesPassed = obstaclesPassed,
            difficulty = difficulty,
            currentSpeed = speed,
            screenShakeIntensity = newShake,
            collisionImpactX = collisionPoint?.first ?: state.collisionImpactX,
            collisionImpactY = collisionPoint?.second ?: state.collisionImpactY,
            timeElapsedSeconds = state.timeElapsedSeconds + clampedDt
        )

        return nextState to events
    }

    /**
     * Directly offsets the player's position (e.g. from drag gesture).
     */
    fun movePlayerDelta(state: GameState, deltaX: Float): GameState {
        if (state.playState != GamePlayState.RUNNING) return state
        val targetX = state.playerX + deltaX
        return state.copy(playerX = GameCollision.clampPlayerX(targetX, config))
    }

    /**
     * Sets target position or imparts velocity from tap.
     */
    fun nudgePlayer(state: GameState, direction: Float): GameState {
        if (state.playState != GamePlayState.RUNNING) return state
        val step = 0.08f * direction
        val targetX = state.playerX + step
        return state.copy(
            playerX = GameCollision.clampPlayerX(targetX, config),
            playerVelocityX = direction * 0.15f
        )
    }
}
