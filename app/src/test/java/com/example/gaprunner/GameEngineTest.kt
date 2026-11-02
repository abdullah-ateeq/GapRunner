package com.example.gaprunner

import com.example.gaprunner.game.Difficulty
import com.example.gaprunner.game.GameCollision
import com.example.gaprunner.game.GameConfig
import com.example.gaprunner.game.GameEngine
import com.example.gaprunner.game.GamePlayState
import com.example.gaprunner.game.GapGenerator
import com.example.gaprunner.game.Obstacle
import com.example.gaprunner.game.ScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private lateinit var config: GameConfig
    private lateinit var engine: GameEngine

    @Before
    fun setUp() {
        config = GameConfig()
        engine = GameEngine(config)
    }

    // ---------------------------------------------------------
    // 1. Player Boundary Movement Tests
    // ---------------------------------------------------------
    @Test
    fun playerClamping_preventsLeavingLeftEdge() {
        val clamped = GameCollision.clampPlayerX(-0.5f, config)
        val minAllowed = config.borderMargin + (config.playerWidth / 2f)
        assertEquals(minAllowed, clamped, 0.0001f)
    }

    @Test
    fun playerClamping_preventsLeavingRightEdge() {
        val clamped = GameCollision.clampPlayerX(1.8f, config)
        val maxAllowed = 1f - config.borderMargin - (config.playerWidth / 2f)
        assertEquals(maxAllowed, clamped, 0.0001f)
    }

    @Test
    fun movePlayerDelta_strictlyEnforcesBoundaries() {
        val state = engine.createInitialState().copy(playState = GamePlayState.RUNNING)

        // Try pushing player far left
        val movedFarLeft = engine.movePlayerDelta(state, -1.0f)
        val minAllowed = config.borderMargin + (config.playerWidth / 2f)
        assertEquals(minAllowed, movedFarLeft.playerX, 0.0001f)

        // Try pushing player far right
        val movedFarRight = engine.movePlayerDelta(state, 1.0f)
        val maxAllowed = 1f - config.borderMargin - (config.playerWidth / 2f)
        assertEquals(maxAllowed, movedFarRight.playerX, 0.0001f)
    }

    @Test
    fun nudgePlayer_respectsDirection() {
        val state = engine.createInitialState().copy(playState = GamePlayState.RUNNING, playerX = 0.5f)
        val nudgedLeft = engine.nudgePlayer(state, -1.0f)
        assertTrue(nudgedLeft.playerX < 0.5f)

        val nudgedRight = engine.nudgePlayer(state, 1.0f)
        assertTrue(nudgedRight.playerX > 0.5f)
    }

    // ---------------------------------------------------------
    // 2. Gap Generation Tests
    // ---------------------------------------------------------
    @Test
    fun generateNextObstacle_staysWithinPlayfieldMargins() {
        val random = Random(42)
        for (obstaclesPassed in listOf(0, 5, 12, 25)) {
            repeat(30) {
                val obs = GapGenerator.generateNextObstacle(
                    id = it.toLong(),
                    y = 0.1f,
                    previousGapCenter = 0.5f,
                    obstaclesPassed = obstaclesPassed,
                    config = config,
                    random = random
                )
                assertTrue("Gap left bound violation", obs.gapLeft >= config.borderMargin)
                assertTrue("Gap right bound violation", obs.gapRight <= 1f - config.borderMargin + 0.0001f)
            }
        }
    }

    @Test
    fun gapJumpLimit_preventsExtremeSuddenJumpsEarlyGame() {
        val random = Random(123)
        var prevCenter: Float? = 0.5f
        val maxEarlyJump = GapGenerator.calculateMaxGapJump(0)

        repeat(20) {
            val obs = GapGenerator.generateNextObstacle(
                id = it.toLong(),
                y = 0.1f,
                previousGapCenter = prevCenter,
                obstaclesPassed = 2, // Early game
                config = config,
                random = random
            )
            val jump = kotlin.math.abs(obs.gapCenter - prevCenter!!)
            assertTrue(
                "Jump $jump exceeded early limit $maxEarlyJump",
                jump <= maxEarlyJump + 0.0001f
            )
            prevCenter = obs.gapCenter
        }
    }

    // ---------------------------------------------------------
    // 3. Gap Width Limits Tests
    // ---------------------------------------------------------
    @Test
    fun gapWidth_neverFallsBelowMinimumOrExceedsMaximum() {
        val random = Random(999)
        for (i in 0..50) {
            val width = GapGenerator.calculateGapWidth(i, config, random)
            assertTrue("Gap width $width is below minimum", width >= config.minGapWidth)
            assertTrue("Gap width $width exceeds maximum", width <= config.maxGapWidth)
            // Ensure minimum gap always comfortably fits the player
            assertTrue("Gap must fit player width", width > config.playerWidth * 1.5f)
        }
    }

    @Test
    fun gapWidth_graduallyReducesWithProgression() {
        val random = Random(101)
        val earlyAvg = (0 until 20).map { GapGenerator.calculateGapWidth(1, config, random) }.average()
        val hardAvg = (0 until 20).map { GapGenerator.calculateGapWidth(25, config, random) }.average()
        assertTrue("Hard gaps ($hardAvg) must be narrower on average than early gaps ($earlyAvg)", hardAvg < earlyAvg)
    }

    // ---------------------------------------------------------
    // 4. Collision Detection Tests
    // ---------------------------------------------------------
    @Test
    fun collision_occursWhenPlayerOutsideGapHorizontally() {
        val obstacle = Obstacle(
            id = 1,
            y = config.playerY, // Overlapping vertically
            gapLeft = 0.40f,
            gapWidth = 0.20f, // Gap is [0.40, 0.60]
            thickness = config.obstacleThickness
        )

        // Player to the left of the gap
        val collideLeft = GameCollision.isColliding(
            playerX = 0.25f,
            playerY = config.playerY,
            obstacle = obstacle,
            config = config
        )
        assertTrue("Player at 0.25f should collide with obstacle left segment", collideLeft)

        // Player to the right of the gap
        val collideRight = GameCollision.isColliding(
            playerX = 0.75f,
            playerY = config.playerY,
            obstacle = obstacle,
            config = config
        )
        assertTrue("Player at 0.75f should collide with obstacle right segment", collideRight)
    }

    @Test
    fun collision_doesNotOccurWhenPlayerCenteredInsideGap() {
        val obstacle = Obstacle(
            id = 1,
            y = config.playerY,
            gapLeft = 0.40f,
            gapWidth = 0.20f, // Gap is [0.40, 0.60]
            thickness = config.obstacleThickness
        )

        val safe = GameCollision.isColliding(
            playerX = 0.50f, // Center of gap
            playerY = config.playerY,
            obstacle = obstacle,
            config = config
        )
        assertFalse("Player centered in gap should survive without collision", safe)
    }

    @Test
    fun collision_doesNotOccurWhenNoVerticalOverlap() {
        val obstacle = Obstacle(
            id = 1,
            y = 0.30f, // Far above player at 0.82f
            gapLeft = 0.40f,
            gapWidth = 0.20f,
            thickness = config.obstacleThickness
        )

        val safe = GameCollision.isColliding(
            playerX = 0.15f, // Misaligned horizontally, but obstacle is far away
            playerY = config.playerY,
            obstacle = obstacle,
            config = config
        )
        assertFalse("Should not collide when vertically distant", safe)
    }

    // ---------------------------------------------------------
    // 5. Successful Obstacle Passing Tests
    // ---------------------------------------------------------
    @Test
    fun hasPassedPlayer_correctlyIdentifiesWhenCleared() {
        val clearedObstacle = Obstacle(
            id = 1,
            y = config.playerY + 0.1f, // Moved past player
            gapLeft = 0.45f,
            gapWidth = 0.2f,
            thickness = config.obstacleThickness
        )

        assertTrue(GameCollision.hasPassedPlayer(config.playerY, clearedObstacle))

        val approachingObstacle = Obstacle(
            id = 2,
            y = config.playerY - 0.1f, // Still approaching
            gapLeft = 0.45f,
            gapWidth = 0.2f,
            thickness = config.obstacleThickness
        )

        assertFalse(GameCollision.hasPassedPlayer(config.playerY, approachingObstacle))
    }

    // ---------------------------------------------------------
    // 6. Score and Streak Calculation Tests
    // ---------------------------------------------------------
    @Test
    fun scoreAndStreak_incrementsCorrectly() {
        val obstacle = Obstacle(id = 1, y = 0.85f, gapLeft = 0.40f, gapWidth = 0.20f)

        // Pass 1: streak goes from 0 to 1
        val eval1 = ScoreCalculator.evaluatePass(playerX = 0.50f, obstacle = obstacle, currentStreak = 0)
        assertEquals(1, eval1.streak)
        assertEquals(1.0f, eval1.streakMultiplier, 0.001f)
        // Perfectly centered: 100 base + 50 perfect center bonus = 150
        assertEquals(150, eval1.pointsAwarded)
        assertTrue(eval1.isPerfectCenter)

        // Pass 2: off center
        val eval2 = ScoreCalculator.evaluatePass(playerX = 0.44f, obstacle = obstacle, currentStreak = 5)
        assertEquals(6, eval2.streak)
        assertEquals(1.5f, eval2.streakMultiplier, 0.001f)
        assertEquals(150, eval2.pointsAwarded) // 100 * 1.5 = 150 (no perfect center bonus)
        assertFalse(eval2.isPerfectCenter)
    }

    @Test
    fun streakMultiplier_scalesWithStreakLevel() {
        assertEquals(1.0f, ScoreCalculator.getStreakMultiplier(1), 0.01f)
        assertEquals(1.25f, ScoreCalculator.getStreakMultiplier(4), 0.01f)
        assertEquals(1.5f, ScoreCalculator.getStreakMultiplier(8), 0.01f)
        assertEquals(2.0f, ScoreCalculator.getStreakMultiplier(12), 0.01f)
        assertEquals(2.5f, ScoreCalculator.getStreakMultiplier(16), 0.01f)
        assertEquals(3.0f, ScoreCalculator.getStreakMultiplier(22), 0.01f)
    }

    // ---------------------------------------------------------
    // 7. Difficulty Progression Tests
    // ---------------------------------------------------------
    @Test
    fun difficulty_transitionsSmoothly() {
        assertEquals(Difficulty.EASY, ScoreCalculator.determineDifficulty(0))
        assertEquals(Difficulty.EASY, ScoreCalculator.determineDifficulty(5))
        assertEquals(Difficulty.MEDIUM, ScoreCalculator.determineDifficulty(6))
        assertEquals(Difficulty.MEDIUM, ScoreCalculator.determineDifficulty(15))
        assertEquals(Difficulty.HARD, ScoreCalculator.determineDifficulty(16))
        assertEquals(Difficulty.HARD, ScoreCalculator.determineDifficulty(40))
    }

    @Test
    fun speed_increasesWithObstaclesPassed() {
        val speed0 = ScoreCalculator.calculateSpeed(0, config)
        val speed10 = ScoreCalculator.calculateSpeed(10, config)
        val speed30 = ScoreCalculator.calculateSpeed(30, config)

        assertEquals(config.baseObstacleSpeed, speed0, 0.001f)
        assertTrue(speed10 > speed0)
        assertTrue(speed30 >= config.maxObstacleSpeed)
    }
}
